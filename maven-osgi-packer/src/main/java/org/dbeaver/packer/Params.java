package org.dbeaver.packer;

import picocli.CommandLine;

import java.nio.file.Path;

public class Params {
    @CommandLine.Option(names = "-f", description = "Path to configuration properties file", required = true)
    public Path target;

    public CommandLine.ParseResult init(String[] args) {
        return new CommandLine(this)
            .parseArgs(args);
    }
}
