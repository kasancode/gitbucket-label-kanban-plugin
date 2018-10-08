# gitbucket-label-kanban-plugin

A [GitBucket](https://github.com/gitbucket/gitbucket) plugin for Kanban style issue management.  
The columns are labels with prefix "@".   

![Screenshot](./doc/screenshot.png)


## Installation

Download jar file from [the release page](https://github.com/kasancode/gitbucket-gantt-plugin/releases) and put into `GITBUCKET_HOME/plugins`.

## Version

Plugin version|GitBucket version
:---|:---
1.0.0|4.26.x

## Build from source

`$ sbt package`

## usage

![labelList](./doc/labels.png)

1. Add label(s) with prefix "@".
1. Move to Kanban page.
1. Drag Issues to another column.

