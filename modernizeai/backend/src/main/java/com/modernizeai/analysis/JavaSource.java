package com.modernizeai.analysis;

import com.github.javaparser.ast.CompilationUnit;
import java.util.List;
import java.util.Optional;

/**
 * A Java source file. {@code unit} is empty when JavaParser could not parse the file; rules then
 * fall back to the regex-extracted imports.
 */
public record JavaSource(String path, String content, Optional<CompilationUnit> unit, List<ImportRef> imports, String parseError) {

    public record ImportRef(String name, int line, boolean isStatic, boolean wildcard) {}

    public boolean parsed() {
        return unit.isPresent();
    }
}
