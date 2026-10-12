package com.modernizeai.parser;

import com.modernizeai.model.DependencyInfo;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses pom.xml with a SAX parser that builds a DOM annotated with line numbers. DOCTYPE
 * declarations and external entities are disabled to prevent XXE attacks.
 */
@Component
public class PomParser {

    static final String LINE_KEY = "modernizeai.line";
    private static final Pattern PROPERTY_REF = Pattern.compile("\\$\\{([^}]+)}");

    public PomModel parse(String path, String xml) throws PomParseException {
        if (xml == null || xml.isBlank()) {
            throw new PomParseException(path + " is empty.", null);
        }
        Document doc = parseWithLines(path, xml);
        Element project = doc.getDocumentElement();
        if (project == null || !"project".equals(localName(project))) {
            throw new PomParseException(path + " does not have a <project> root element.", 1);
        }

        Map<String, PomModel.Property> props = new LinkedHashMap<>();
        Element propsEl = child(project, "properties");
        if (propsEl != null) {
            for (Element p : children(propsEl)) {
                props.put(localName(p), new PomModel.Property(localName(p), text(p), line(p)));
            }
        }

        PomModel.Parent parent = null;
        Element parentEl = child(project, "parent");
        if (parentEl != null) {
            parent = new PomModel.Parent(childText(parentEl, "groupId"), childText(parentEl, "artifactId"),
                    childText(parentEl, "version"), line(parentEl));
        }

        String groupId = childText(project, "groupId");
        String version = childText(project, "version");
        Resolver resolver = new Resolver(props, version != null ? version : parent != null ? parent.version() : null,
                parent != null ? parent.version() : null,
                groupId != null ? groupId : parent != null ? parent.groupId() : null);

        List<DependencyInfo> deps = readDependencies(child(project, "dependencies"), resolver, false);
        Element dm = child(project, "dependencyManagement");
        List<DependencyInfo> managed = dm == null ? List.of() : readDependencies(child(dm, "dependencies"), resolver, true);

        List<PomModel.Plugin> plugins = new ArrayList<>();
        Element build = child(project, "build");
        if (build != null) {
            plugins.addAll(readPlugins(child(build, "plugins"), resolver));
            Element pm = child(build, "pluginManagement");
            if (pm != null) plugins.addAll(readPlugins(child(pm, "plugins"), resolver));
        }

        List<String> modules = new ArrayList<>();
        Element modulesEl = child(project, "modules");
        if (modulesEl != null) {
            for (Element m : children(modulesEl)) modules.add(text(m));
        }

        return new PomModel(path, groupId != null ? groupId : parent != null ? parent.groupId() : null,
                childText(project, "artifactId"), version != null ? version : parent != null ? parent.version() : null,
                childText(project, "packaging"), parent, props, deps, managed, plugins, modules);
    }

    /** Resolves ${...} references using properties declared in the same POM only. */
    public static String resolve(String value, Map<String, PomModel.Property> props) {
        return new Resolver(props, null, null, null).resolve(value);
    }

    private List<DependencyInfo> readDependencies(Element container, Resolver resolver, boolean managed) {
        List<DependencyInfo> out = new ArrayList<>();
        if (container == null) return out;
        for (Element d : children(container)) {
            if (!"dependency".equals(localName(d))) continue;
            String v = childText(d, "version");
            out.add(new DependencyInfo(resolver.resolve(childText(d, "groupId")), resolver.resolve(childText(d, "artifactId")),
                    v, resolver.resolve(v), childText(d, "scope"), childText(d, "type"), line(d), managed));
        }
        return out;
    }

    private List<PomModel.Plugin> readPlugins(Element container, Resolver resolver) {
        List<PomModel.Plugin> out = new ArrayList<>();
        if (container == null) return out;
        for (Element p : children(container)) {
            if (!"plugin".equals(localName(p))) continue;
            Map<String, PomModel.Property> config = new LinkedHashMap<>();
            Element cfg = child(p, "configuration");
            if (cfg != null) {
                for (Element c : children(cfg)) {
                    String t = text(c);
                    config.put(localName(c), new PomModel.Property(localName(c), resolver.resolve(t), line(c)));
                }
            }
            String gid = childText(p, "groupId");
            out.add(new PomModel.Plugin(gid == null ? "org.apache.maven.plugins" : gid, childText(p, "artifactId"),
                    resolver.resolve(childText(p, "version")), line(p), config));
        }
        return out;
    }

    private Document parseWithLines(String path, String xml) throws PomParseException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            SAXParser parser = spf.newSAXParser();
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().newDocument();
            parser.parse(new InputSource(new StringReader(xml)), new LineHandler(doc));
            return doc;
        } catch (SAXParseException e) {
            throw new PomParseException(path + " is not well-formed XML (line " + e.getLineNumber() + "): "
                    + sanitize(e.getMessage()), e.getLineNumber() > 0 ? e.getLineNumber() : null);
        } catch (SAXException | ParserConfigurationException | java.io.IOException e) {
            throw new PomParseException(path + " could not be parsed as XML.", null);
        }
    }

    private static String sanitize(String msg) {
        if (msg == null) return "";
        String s = msg.replaceAll("[\\r\\n]+", " ");
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    /** SAX handler that builds a DOM and stores each element's start line as user data. */
    private static final class LineHandler extends DefaultHandler {
        private final Document doc;
        private final java.util.Deque<Element> stack = new java.util.ArrayDeque<>();
        private final StringBuilder textBuf = new StringBuilder();
        private Locator locator;

        LineHandler(Document doc) {
            this.doc = doc;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            flushText();
            Element el = doc.createElementNS(uri == null || uri.isEmpty() ? null : uri, qName);
            el.setUserData(LINE_KEY, locator != null ? locator.getLineNumber() : null, null);
            if (stack.isEmpty()) doc.appendChild(el);
            else stack.peek().appendChild(el);
            stack.push(el);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            flushText();
            stack.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            textBuf.append(ch, start, length);
        }

        private void flushText() {
            if (textBuf.length() > 0 && !stack.isEmpty()) {
                stack.peek().appendChild(doc.createTextNode(textBuf.toString()));
            }
            textBuf.setLength(0);
        }
    }

    private static final class Resolver {
        private final Map<String, PomModel.Property> props;
        private final Map<String, String> builtins = new LinkedHashMap<>();

        Resolver(Map<String, PomModel.Property> props, String projectVersion, String parentVersion, String groupId) {
            this.props = props;
            if (projectVersion != null) {
                builtins.put("project.version", projectVersion);
                builtins.put("pom.version", projectVersion);
                builtins.put("version", projectVersion);
            }
            if (parentVersion != null) builtins.put("project.parent.version", parentVersion);
            if (groupId != null) builtins.put("project.groupId", groupId);
        }

        String resolve(String value) {
            if (value == null) return null;
            String current = value;
            for (int depth = 0; depth < 10 && current.contains("${"); depth++) {
                Matcher m = PROPERTY_REF.matcher(current);
                StringBuilder sb = new StringBuilder();
                boolean changed = false;
                while (m.find()) {
                    String key = m.group(1);
                    String replacement = builtins.get(key);
                    if (replacement == null && props.containsKey(key)) replacement = props.get(key).value();
                    if (replacement == null) {
                        m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                    } else {
                        m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                        changed = true;
                    }
                }
                m.appendTail(sb);
                current = sb.toString();
                if (!changed) break;
            }
            return current.contains("${") ? null : current;
        }
    }

    // ---- small DOM helpers ----

    static String localName(Node n) {
        String ln = n.getLocalName();
        return ln != null ? ln : n.getNodeName();
    }

    static Integer line(Element e) {
        Object v = e.getUserData(LINE_KEY);
        return v instanceof Integer i ? i : null;
    }

    static List<Element> children(Element parent) {
        List<Element> out = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element e) out.add(e);
        }
        return out;
    }

    static Element child(Element parent, String name) {
        for (Element e : children(parent)) {
            if (name.equals(localName(e))) return e;
        }
        return null;
    }

    static String childText(Element parent, String name) {
        Element c = child(parent, name);
        return c == null ? null : text(c);
    }

    static String text(Element e) {
        String t = e.getTextContent();
        if (t == null) return null;
        t = t.trim();
        return t.isEmpty() ? null : t;
    }
}
