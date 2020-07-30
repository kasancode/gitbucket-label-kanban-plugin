
name := "gitbucket-label-kanban-plugin"
organization := "io.github.gitbucket"
version := "3.6.0"
scalaVersion := "2.13.0"
gitbucketVersion := "4.34.0"

lazy val root = (project in file("."))
  .enablePlugins(SbtTwirl)
