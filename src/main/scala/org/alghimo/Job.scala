package org.alghimo

object Job {
    def main(args: Array[String]) {
        if (!args.isEmpty && args(0) == "update-history") {
            println("Running in update-history mode")
            UpdateHistoryJob.run(args.tail)
        } else {
            println("Running in real-time mode")
            ScoringJob.run(args)
        }
    }
}
