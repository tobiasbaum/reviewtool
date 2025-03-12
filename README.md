# CoRT - Code Review Tool

CoRT's aim is to spear-head a new generation of "cognitive support code review tools", that is review tools that are not simply tools to annotate portions of source code but that rather help the reviewer to understand the source code and to reduce the cognitive load. It is intended to be an industrial strength code review tool, as well as a platform for code review research.

CoRT is built to support "change-based code review", which means that the portions of the code that have to be reviewed are extracted from changes in a source code repository. It currently supports Subversion, but is extensible in this regard.

Modern IDEs already provide a lot of support in understanding source code (linking between caller and callee, syntax highlighting, ...). Therefore CoRT is implemented as a plugin for the Eclipse IDE.

## Installation

Download the Eclipse update site zip from the "releases" page or build it yourself by calling "./mvnw install". Then install it to Eclipse in the usual way.

## Configuration

CoRT is usually configured for a whole team. Therefore, it has an XML configuration file that can be committed to version control and that is referenced from Eclipse's settings dialog.

- The settings dialog can be found under "Window -> Preferences -> Reviewtool". You need to reference a configuration file there.
- The configuration file can contain placeholders, for example for user names. These need to be configured in the settings dialog, too.
- If a placeholder starts with the prefix "env." (i. e. "${env.USERNAME}"), the placeholder will be replaced with the value of a environment variable (environment variable "USERNAME" in this case).
- Two examples for configuration files can be found in the repository root ("testconfig1.xml" and "testconfig2.xml"). You need to adjust them for your specific situation.
- There is not much documentation for the config format at the moment. If you want to get into the details, have a look at the various subclasses of de.setsoftware.reviewtool.config.IConfigurator from the ...core project.

## The research

CoRT is built at the "Fachgebiet Software Engineering" of Leibniz University Hannover (http://se.uni-hannover.de). The principles behind CoRT were derived using sound research methodology, CoRT is evaluated in a research project, and is also used to provide data for code review research. Most of the research results can be found on the university homepage or at http://tobias-baum.de

## The name

Quite obviously, CoRT stands for "Code Review Tool". But as it is fashionable to name review tools after people's names (like Mondrian, Rietveld and Gerrit), we also have a corresponding explanation. In fact, we even have two:
* CoRT sounds similar to Cord. Cord Broyhan was a famous beer brewer in Hannover in the middle ages: https://de.wikipedia.org/wiki/Cord_Broyhan
* CoRT sounds similar to Kurt. Kurt Schwitters was an influential artist from Hannover: https://de.wikipedia.org/wiki/Kurt_Schwitters

[![Build Status](https://travis-ci.com/tobiasbaum/reviewtool.svg?branch=master)](https://app.travis-ci.com/tobiasbaum/reviewtool)
