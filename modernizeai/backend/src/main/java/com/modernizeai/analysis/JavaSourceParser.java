package com.modernizeai.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Parses Java source text into an AST. Never compiles or executes the code. */
@Component
public class JavaSourceParser {

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(static\\s+)?([\\w.]+)(\\.\\*)?\\s*;");

    public JavaSource parse(String path, String content) {
        // A new parser per call: JavaParser instances are not thread-safe.
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setAttributeComments(false));
        try {
            ParseResult<CompilationUnit> result = parser.parse(content);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                List<JavaSource.ImportRef> imports = new ArrayList<>();
                for (ImportDeclaration imp : cu.getImports()) {
                    int line = imp.getBegin().map(p -> p.line).orElse(0);
                    imports.add(new JavaSource.ImportRef(imp.getNameAsString(), line, imp.isStatic(), imp.isAsterisk()));
                }
                return new JavaSource(path, content, Optional.of(cu), imports, null);
            }
            String problem = result.getProblems().isEmpty() ? "unknown syntax error" : result.getProblem(0).getVerboseMessage();
            return fallback(path, content, problem);
        } catch (RuntimeException | StackOverflowError e) {
            return fallback(path, content, "parser failure: " + e.getClass().getSimpleName());
        }
    }

    private JavaSource fallback(String path, String content, String problem) {
        List<JavaSource.ImportRef> imports = new ArrayList<>();
        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = IMPORT.matcher(lines[i]);
            if (m.find()) {
                imports.add(new JavaSource.ImportRef(m.group(2), i + 1, m.group(1) != null, m.group(3) != null));
            }
        }
        String first = problem.split("\\R")[0];
        return new JavaSource(path, content, Optional.empty(), imports, first.length() > 200 ? first.substring(0, 200) : first);
    }
}
