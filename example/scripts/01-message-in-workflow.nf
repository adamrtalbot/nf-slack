#!/usr/bin/env nextflow

/*
 * Simple example demonstrating nf-slack plugin
 *
 * This workflow sends Slack messages:
 * - BEFORE: At workflow start
 * - DURING: While processing
 * - AFTER: At workflow completion
 */

// Import the Slack messaging function
include { slackMessage } from 'plugin/nf-slack'

process HELLO {
    input:
    val sample_id

    output:
    stdout

    script:
    """
    echo "Processing sample: ${sample_id}"
    sleep 2  # Simulate some work
    echo "${sample_id}_processed"
    """
}

workflow {

    inputs = channel.of('sample_1', 'sample_2', 'sample_3')
    HELLO(inputs)

    // ==============================================================
    // AFTER: Send rich formatted completion message in workflow body
    // ==============================================================
    slackMessage([
        message: "Example workflow complete! 🎉",
        fields: [
            [
                title: "Status",
                value: "Success",
                short: true
            ],
            [
                title: "Samples",
                value: "3",
                short: true
            ]
        ]
    ])
}
