# Installation

This guide covers installing the nf-slack plugin in your Nextflow pipeline.

## Prerequisites

- Nextflow v25.04.0 or later
- A Slack webhook URL (see [Webhook Setup guide](webhook-setup.md))

## Adding the Plugin

Add the nf-slack plugin to your `nextflow.config`:

```groovy
plugins {
    id 'nf-slack@0.1.0'
}
```

!!! tip "Using Multiple Plugins?"

    If you already have a `plugins` block, just add the nf-slack entry:

    ```groovy
    plugins {
        id 'nf-validation'
        id 'nf-slack@0.1.0'  // Add this line
    }
    ```

## Specifying a Version

You can specify a particular version of the plugin:

```groovy
plugins {
    id 'nf-slack@0.1.0'  // Use a specific version
}
```

To use the latest version, omit the version number:

```groovy
plugins {
    id 'nf-slack'  // Uses the latest version
}
```

## Verification

To verify the plugin is installed correctly, run:

```bash
nextflow plugin list
```

You should see `nf-slack` in the list of installed plugins.

## Local Installation (Development)

If you want to install a local development version of the plugin:

1. **Clone the repository**

   ```bash
   git clone https://github.com/adamrtalbot/nf-slack.git
   cd nf-slack
   ```

2. **Install locally**

   ```bash
   make install
   ```

This will build and install the plugin to your local Nextflow plugins directory. For more details on development setup, see the [Contributing guide](../contributing.md).

## Next Steps

- [Set up your Slack webhook](webhook-setup.md)
- [Get started with basic notifications](quick-start.md)
- [Configure the plugin](../usage/configuration.md)
