#!/usr/bin/env nextflow

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
        // =====================================================================
        // DURING: Send rich formatted completion message within `map` operation
        // =====================================================================
        .map { sample ->
            // Send a simple message for each item
            slackMessage("⚙️ Processing ${sample}")
            return sample
        }
    HELLO(inputs)
}
