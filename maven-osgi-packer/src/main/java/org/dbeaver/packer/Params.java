package org.dbeaver.packer;

import picocli.CommandLine;

import java.nio.file.Path;

public class Params {
    @CommandLine.Option(names = "-f", description = "Path to configuration properties file", required = true)
    public Path target;

    public void init(String[] args) {
        new CommandLine(this).parseArgs(args);
    }
}
