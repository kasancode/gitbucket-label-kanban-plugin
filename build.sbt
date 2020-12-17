
name := "gitbucket-label-kanban-plugin"
organization := "io.github.gitbucket"
version := "3.7.0"
scalaVersion := "2.13.1"
gitbucketVersion := "4.35.0"

lazy val root = (project in file("."))
  .enablePlugins(SbtTwirl)
