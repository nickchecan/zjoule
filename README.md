<h1 align="center">
<img src="assets/zjoule-icon.png" alt="zJoule" width="25"/> zjoule
</h1>

<div align="center">

[![Latest Test](https://github.com/nickchecan/zjoule/actions/workflows/main.yaml/badge.svg)](https://github.com/nickchecan/zjoule/actions/workflows/main.yaml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nickchecan_zjoule&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=nickchecan_zjoule)
[![CodeQL](https://github.com/nickchecan/zjoule/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/nickchecan/zjoule/actions/workflows/github-code-scanning/codeql)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://zjoule.com/v2.0.2/doc/)
[![semantic-release: angular](https://img.shields.io/badge/semantic--release-angular-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release)

</div>

> [!IMPORTANT]
> :construction: This repository is passing through important changes and during this time, the plugin won't be available. <br />

**zJoule** is an Open Source AI assistant plugin for Eclipse designed to integrate Generative AI into your development environment.

## Documentation Content
- [Requirements](#requirements)
- [Compatibility](#compatibility)
- [Motivation](#motivation)
- [Getting Started](#getting-started)
- [Demo](#demo)
- [How to Contribute](#how-to-contribute)
- [References](#reference)
- [License](#license)

## Requirements
- A local **Ollama** installation or **SAP AI Core** with the `Extended` service plan (for the Generative AI Hub enablement).
- Eclipse 2024-09 (4.33.0) or latest. *Compatibility with older versions is not guaranteed.*

## Compatibility

The following table outlines the Generative AI models currently compatible with this plugin, as well as the next models that will soon be available.

> **Privacy Information** <br>
We know how privacy is important and ensure that all communication handled by the plugin is secure **is a must**. Here's how your data is managed:
> - **Data Flow:** All communication facilitated by the plugin occurs exclusively between Eclipse and the SAP AI Core model APIs. No external intermediaries are involved.
> - **Data Storage:** Any data processed or generated during plugin usage is stored locally within Eclipse's memory mechanism, ensuring it remains private and confined to your development environment. <br>
>
> By keeping all operations local and secure, the plugin should provide a trustworthy and seamless experience.

### Ollama

This plugin supports Ollama models through a local installation. For more information regarding the models available, please, check [this link]().

### SAP AI Core Generative AI Hub

| Executable ID | Model                          | Version                   | Compatibility      |
|---------------|--------------------------------|---------------------------|:------------------:|
| azure-openai  | `gpt-35-turbo`                 | 0613                      | :white_check_mark: |
| azure-openai  | `gpt-35-turbo`                 | 1106 (latest)             | :white_check_mark: | 
| azure-openai  | `gpt-35-turbo-16k`             | 0613 (latest)             | :white_check_mark: |
| azure-openai  | `gpt-4`                        | 0613                      | :white_check_mark: |
| azure-openai  | `gpt-4-32k`                    | 0613 (latest)             | :white_check_mark: |
| azure-openai  | `gpt-4o`                       | 2024-05-13                | :construction:     |
| azure-openai  | `gpt-4o`                       | 2024-08-06 (latest)       | :construction:     |
| azure-openai  | `gpt-4o-mini`                  | 2024-07-18                | :construction:     |
| azure-openai  | `gpt-4`                        | turbo-2024-04-09 (latest) | :construction:     |
| aws-bedrock   | `anthropic--claude-3-haiku`    | 1 (latest)                | :construction:     |
| aws-bedrock   | `anthropic--claude-3-sonnet`   | 1 (latest)                | :construction:     |
| aws-bedrock   | `anthropic--claude-3.5-sonnet` | 1                         | :construction:     |
| aws-bedrock   | `anthropic--claude-3.5-sonnet` | 2 (latest)                | :construction:     |

LLM models that are not compatible may be selected during the login process; however, their functionality cannot be guaranteed, and errors may occur.

Additional model compatibilities may be introduced in the near future :)

## Motivation

The official Joule version for ADT is being rolled out exclusively for S/4HANA Private Cloud and SAP BTP, ABAP Environment at the end of Q1 of 2025. But don’t worry, this plugin was created to bridges the gap! It introduces an AI-powered chat interface that seamlessly connects other types of SAP systems to large language models (LLMs) through different AI providers.

The idea is to open the door to integrate SAP ABAP development with new models and innovative features that could emerge from the expanding possibilities of AI. 

All from the cozy confines of your Eclipse ADT =)

### Benefits

#### 1. Coding Assistance:
- Answer coding-related questions.
- Provide real-time suggestions and explanations for code.
- Assist in debugging by analyzing error messages and suggesting fixes.
#### 2. Documentation & Learning:
- Summarize or explain code snippets and concepts.
- Fetch and clarify information on APIs, libraries, or frameworks.
#### 3. Productivity Boost:
- Reduce context switching by enabling developers to query information directly within Eclipse.
- Automate repetitive tasks through scripting and custom commands.
#### 4. Integration with SAP AI Core:
- Access advanced AI capabilities, potentially tailored to SAP-specific development workflows.
#### 5. Dynamic Testing Support:
- Offer insights into test cases or suggest ways to optimize testing processes.
#### 6. Natural Language Interaction:
- Simplify complex development tasks by allowing users to describe their needs in plain language.
#### 7. Enhanced Development Experience:
- Enable faster learning for beginners or new team members.
- Provide a smoother workflow for experienced developers needing quick solutions.
#### 8. Privacy and Security:
- If using a local Ollama installation, all data processing occurs entirely on your machine, without any internet dependency.
- This ensures maximum privacy, as sensitive code or project details never leave your local environment.

## Getting Started

1. Download and install **zJoule** in your Eclipse instance following the procedure described [right here](https://zjoule.com).

2. If the plugin view is not currently open in your Eclipse Editor, go to `Window > Show View > Other..` and search for **zJoule** inside the **ABAP Copilot** folder.

3. Click on the authentication button (lock icon) at the top of the view and select the AI provider you want to work with.

### Connecting to an Ollama local installation

4. Enter the localhost and port where your Ollama instance is available.

5. Select the model you have installed and want to work with.

### Connecting to an SAP AI Core instance

4. Login using your SAP AI Core `Service Key`.

You can find it in the *Instances an Subscriptions* area of your SAP BTP subaccount.

5. Select the `resource group` and the `deployment` model you want to use.

Resource Groups are essentially a project workspace in the context of SAP AI Core, and contains all components a specific ML or AI solution might use to attend a specific requirement. [More Information](https://help.sap.com/docs/sap-ai-core/sap-ai-core-service-guide/resource-groups?q=resource+groups).

Deployment is the component that will enable a LLM to be consumed. [More Information](https://help.sap.com/docs/sap-ai-core/sap-ai-core-service-guide/create-deployment-for-generative-ai-model-in-sap-ai-core).

## Demo

### Asking for code samples.

zJoule offers code snippets and on-the-go assistance, helping you learn or recall information without ever leaving your development environment.

### Requesting context information.

When the user opens a new component, such as a class or a CDS view, zJoule will retrieve its content and include it as context for the user prompt. A small tag will appear above the input box, indicating the content being used as context.

### Clean up chat history.

As the chat progresses, additional information will be collected and shared with the AI assistant to enhance personalization. However, the user can clear the chat history at any time if the accumulated messages are no longer relevant to the conversation.

### Logout from the current SAP AI Core instance.

Logging out will erase the chat history and any login data used during the LLM communication process, enabling users to establish new connections entirely independent of previous interactions.

## How to Contribute

Contributions are welcome! Here's how you can get involved:

1. **Report Issues:** Found a bug or have a feature request? [Open an issue](https://github.com/nickchecan/zjoule/issues). <br />
2. **Submit Pull Requests:** Fork the repository, create a new branch, make your changes, and submit a PR. <br />
3. **Improve Documentation:** Help us improve the README or add examples to make setup easier. <br />
4. **Test & Feedback:** Try this Eclipse AI Assistant and give us feedback to improve them.

Please follow the [contribution guidelines](CONTRIBUTING.md) for more details.

## Reference

[SAP Note: 3437766 (Availability of Generative AI Models)](https://me.sap.com/notes/3437766)

[How this project consumes generative AI models.](https://help.sap.com/docs/sap-ai-core/sap-ai-core-service-guide/consume-generative-ai-models-using-sap-ai-core)

[The spinner "Ellipsis" is provided by loading.io](https://loading.io/icon/)

[zJoule - ABAP Copilot website repository](https://github.com/nickchecan/zjoule-website)

[zJoule - ABAP Copilot website](https://zjoule.com)

[Ollama API documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)

[Ollama website](https://ollama.com)

## License
Copyright (c) 2024 Nicholas Coutinho Checan. <br />
Licensed under the MIT License. See [LICENSE](LICENSE).
