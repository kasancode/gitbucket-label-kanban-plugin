
name := "gitbucket-label-kanban-plugin"
organization := "io.github.gitbucket"
version := "3.7.1"
scalaVersion := "2.13.8"
gitbucketVersion := "4.38.0"

lazy val root = (project in file("."))
  .enablePlugins(SbtTwirl)
