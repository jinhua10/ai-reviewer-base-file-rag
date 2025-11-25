# Prompt Template Configuration Guide

## Overview

Starting from version 1.0, the system supports customizing LLM Prompt templates through configuration files without modifying the code. You can adjust AI response style and requirements easily.

## Configuration Location

**Config File**: `config/application.yml`

**Config Path**: `knowledge.qa.llm.prompt-template`

## Configuration Format

```yaml
knowledge:
  qa:
    llm:
      # Prompt template
      # Supports two placeholders:
      #   - {question}: User's question
      #   - {context}: Related document content
      prompt-template: |
        You are a professional knowledge assistant. Please answer user questions based on document content.
        
        # Response Requirements
        1. Must answer based on document content, do not fabricate information
        2. If no relevant information is found in documents, clearly inform the user
        3. Answers should be clear, accurate, and well-organized
        4. You can cite document names as information sources
        5. Maintain a professional and friendly tone
        
        # User Question
        {question}
        
        # Related Documents
        {context}
        
        # Please provide your answer:
```

## Placeholder Description

The prompt template supports the following placeholders that will be automatically replaced at runtime:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{question}` | User's question | "What is RAG technology?" |
| `{context}` | Retrieved relevant document content | Text snippets from relevant documents |

## Customization Examples

### Example 1: Customer Service Style

```yaml
prompt-template: |
  You are a friendly customer service assistant. Please answer customer questions based on product documentation.
  
  Requirements:
  - Use simple and easy-to-understand language
  - Be patient and polite
  - If the answer is not found in documents, guide customers to contact human support
  
  Customer Question: {question}
  
  Related Documents:
  {context}
  
  Please provide a professional and friendly answer:
```

### Example 2: Technical Documentation Style

```yaml
prompt-template: |
  You are a professional technical documentation assistant. Answer questions based on the following technical documents.
  
  Requirements:
  1. Answers must be accurate, professional, and technical
  2. Include specific technical details and examples
  3. Provide code snippets or commands when applicable
  4. Cite document sources
  
  Question: {question}
  
  Related Technical Documents:
  {context}
  
  Technical Answer:
```

### Example 3: Educational Style

```yaml
prompt-template: |
  You are a patient educational tutor. Please explain the answer in an easy-to-understand way.
  
  Teaching Requirements:
  - Explain step by step, from shallow to deep
  - Use analogies and examples to aid understanding
  - Encourage student thinking
  - Answer based on textbook content
  
  Student Question: {question}
  
  Textbook Content:
  {context}
  
  Please provide educational explanation:
```

### Example 4: Concise Q&A Style

```yaml
prompt-template: |
  Answer the question based on the following documents, keep it concise and clear.
  
  Question: {question}
  
  Documents: {context}
  
  Answer:
```

## Best Practices

### 1. Recommended Prompt Structure

A good prompt structure should include:

1. **Role Definition**: Clearly define AI's role (e.g., professional assistant, customer service, teacher)
2. **Response Requirements**: List specific response guidelines and precautions
3. **Question Section**: Use `{question}` placeholder
4. **Context Section**: Use `{context}` placeholder
5. **Output Guidance**: Prompt AI to start generating response

### 2. Prompt Optimization Tips

- **Clear and Specific**: More specific requirements lead to better answers
- **Numbered Lists**: Use numbers or symbols to list requirements for better AI understanding
- **Example Guidance**: If specific format is needed, provide examples in the prompt
- **Constraints**: Clearly state what can and cannot be done
- **Tone Control**: Influence AI's response tone and style through wording

### 3. Debugging and Testing

1. After modifying the config file, restart the application for changes to take effect
2. Test different questions through the Web interface to evaluate response quality
3. Adjust the prompt based on feedback and gradually optimize
4. Save different versions of prompt configurations for comparison testing

## How to Use

### 1. View Current Configuration

Edit `config/application.yml`, locate:

```yaml
knowledge.qa.llm.prompt-template
```

### 2. Customize Prompt

Modify the `prompt-template` value, for example:

```yaml
prompt-template: |
  You are a friendly customer service assistant.
  
  Question: {question}
  Documents: {context}
  
  Please provide a friendly answer:
```

### 3. Apply Configuration

**Method 1: Restart Application**
```bash
# Windows
stop.bat
start.bat

# Linux/Mac
./stop.sh
./start.sh
```

**Method 2: Start with Different Configuration**
```bash
java -jar ai-reviewer-base-file-rag-1.0.jar --spring.profiles.active=custom
```

### 4. Test Results

Ask questions through the Web interface (http://localhost:8080) and observe if the AI response style meets expectations.

## FAQ

**Q: How long does it take for configuration changes to take effect?**  
A: You need to restart the application. Stop the application (stop.bat), modify the config file, then restart (start.bat).

**Q: Are placeholders mandatory?**  
A: Yes, both `{question}` and `{context}` must exist in the prompt template, otherwise AI cannot access the question and document content.

**Q: Will a very long prompt cause issues?**  
A: The prompt itself consumes LLM's token quota. It's recommended to keep it within 500 characters to avoid impacting context capacity.

**Q: How to restore default configuration?**  
A: Delete the `prompt-template` configuration item, or use the default template from the "Configuration Format" section above.

**Q: Are multilingual prompts supported?**  
A: Yes. You can write prompts in Chinese, English, or other languages, depending on your LLM model's capabilities.

## Related Configuration

Other LLM-related configuration items:

```yaml
knowledge:
  qa:
    llm:
      provider: openai              # LLM provider
      api-key: ${AI_API_KEY:}       # API key
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat          # Model name
      max-context-length: 20000     # Max context length
      max-doc-length: 5000          # Max single document length
      prompt-template: |            # Prompt template (this feature)
        ...
```

## Feature Highlights

### 1. Flexible Configuration

- ✅ No code modification needed, adjust prompts through config file
- ✅ Supports multi-line text format for structured prompts
- ✅ Provides default template, ready to use out of the box

### 2. Placeholder System

Supports two dynamic placeholders:
- `{question}` - Automatically replaced with user's question
- `{context}` - Automatically replaced with retrieved document content

### 3. Scenario Adaptation

Can be customized for different business scenarios:
- Customer service style
- Technical documentation style
- Educational tutoring style
- Concise Q&A style

### 4. Version Management

- Default value provided, ensures backward compatibility
- Supports switching between different configurations via Spring Profile
- Can maintain multiple configuration file versions

---

**Last Updated**: 2025-11-25  
**Version**: v1.0

