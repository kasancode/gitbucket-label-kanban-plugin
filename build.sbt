
name := "gitbucket-label-kanban-plugin"
organization := "io.github.gitbucket"
version := "3.0.1"
scalaVersion := "2.12.8"
gitbucketVersion := "4.29.0"

lazy val root = (project in file("."))
  .enablePlugins(SbtTwirl)
