
name := "gitbucket-label-kanban-plugin"
organization := "io.github.gitbucket"
version := "2.0.3"
scalaVersion := "2.12.6"
gitbucketVersion := "4.25.0"

lazy val root = (project in file("."))
  .enablePlugins(SbtTwirl)
