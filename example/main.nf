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

    // ============================================
    // BEFORE: Send message at workflow start
    // ============================================
    // slackMessage("🚀 Example workflow starting!")

    // ============================================
    // DURING: Create a simple channel and send messages
    // ============================================
    inputs = channel.of('sample_1', 'sample_2', 'sample_3')
        .map { sample ->
            // Send a simple message for each item
            slackMessage("⚙️ Processing ${sample}")
            return sample
        }
    HELLO(inputs)

    // ============================================
    // AFTER: Send rich formatted completion message
    // ============================================
    slackMessage([
        message: "Example workflow complete! 🎉",
        color: "#2EB887",  // Green for success
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

    workflow.onComplete = {
        def status = workflow.success ? '✅ SUCCESS' : '❌ FAILED'
        def color = workflow.success ? '#2EB887' : '#A30301'

        slackMessage([
            message: "Workflow ${status}",
            color: color,
            fields: [
                [
                    title: "Duration",
                    value: "${workflow.duration}",
                    short: true
                ]
            ]
        ])
    }
}