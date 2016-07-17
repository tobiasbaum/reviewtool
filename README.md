# CoRT - Code Review Tool

CoRT's aim is to spear-head a new generation of "cognitive support code review tools", that is review tools that are not simply tools to annotate portions of source code but that rather help the reviewer to understand the source code and to reduce the cognitive load. It is intended to be an industrial strength code review tool, as well as a platform for code review research.

CoRT is built to support "change-based code review", which means that the portions of the code that have to be reviewed are extracted from changes in a source code repository. It currently supports Subversion, but is extensible in this regard.

Modern IDEs already provide a lot of support in understanding source code (linking between caller and callee, syntax highlighting, ...). Therefore CoRT is implemented as a plugin for the Eclipse IDE.

## The research

CoRT is built at the "Fachgebiet Software Engineering" of Leibniz University Hannover (http://se.uni-hannover.de). The principles behind CoRT were derived using sound research methodology, CoRT is evaluated in a research project, and is also used to provide data for code review research. Most of the research results can be found on the university homepage or at http://tobias-baum.de

## The name

Quite obviously, CoRT stands for "Code Review Tool". But as it is fashionable to name review tools after people's names (like Mondrian, Rietveld and Gerrit), we also have a corresponding explanation. In fact, we even have two:
* CoRT sounds similar to Cord. Cord Broyhan was a famous beer brewer in Hannover in the middle ages: https://de.wikipedia.org/wiki/Cord_Broyhan
* CoRT sounds similar to Kurt. Kurt Schwitters was an influential artist from Hannover: https://de.wikipedia.org/wiki/Kurt_Schwitters

[![Build Status](https://travis-ci.org/tobiasbaum/reviewtool.svg?branch=master)](https://travis-ci.org/tobiasbaum/reviewtool)
