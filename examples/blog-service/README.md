# Blog service

## Overview

A backend service for a blog platform that supports the following core features:

1. Submitting blog posts
2. Viewing blog posts by category, sorted by rating

![Blog service](./images/blog_service.png)

## Implementation

This service is developed using Ballerina, an open source, cloud-native programming language optimized for integration. It leverages Ballerina's built-in service and network primitives, strong user-defined types, database connectors, etc. to efficiently implement the backend functionality.

To identify categories and assign ratings - tasks better suited for a large language model (LLM) - the implementation uses Ballerina's natural expressions to seamlessly invoke an LLM and retrieve the required information in the desired format.

### Blog post submission
 
When a blog post is submitted, the system attempts to determine the most appropriate category (from a predefined list) and assign a rating between 1 and 10. If a suitable category is found and the rating exceeds a defined threshold (e.g., 3), the blog post is accepted and persisted along with the identified category and rating.

- The implementation uses an LLM to identify the most suitable category and a rating based on the specified review criteria. This is implemented using natural expressions in Ballerina, specifically a [natural function](./review_blog.bal) with the requirement specified in natural language. Ballerina's natural expressions handle incorporating the response schema in the request to the LLM and binding the response to the expected type automatically. 

    ```ballerina
    # Represents a blog entry with title and content.
    public type Blog record {|
        # The title of the blog
        string title;
        # The content of the blog
        string content;
    |};

    # Review of a blog entry.
    type Review record {|
        # Suggested category
        string? suggestedCategory;
        # Rating out of 10
        int rating;
    |};

    public isolated function reviewBlog(Blog blog) returns Review|error => natural {
        You are an expert content reviewer for a blog site that 
            categorizes posts under the following categories: ${categories}

            Your tasks are:
            1. Suggest a suitable category for the blog from exactly the specified categories. 
            If there is no match, use null.

            2. Rate the blog post on a scale of 1 to 10 based on the following criteria:
            - **Relevance**: How well the content aligns with the chosen category.
            - **Depth**: The level of detail and insight in the content.
            - **Clarity**: How easy it is to read and understand.
            - **Originality**: Whether the content introduces fresh perspectives or ideas.
            - **Language Quality**: Grammar, spelling, and overall writing quality.

            Here is the blog post content:

            Title: ${blog.title}
            Content: ${blog.content}
    };
    ```

- The rest of the logic, including accepting or rejecting the blog depending on the category and/or rating, persisting the data, etc. are all handled entirely in Ballerina.

### Blog post retrieval

Blog posts can be retrieved by category and are automatically sorted by the rating assigned during submission. This retrieval functionality is also implemented entirely in Ballerina.

## Building and running the sample

Ballerina Swan Lake Update 13 (2201.13.0) will include experimental support for natural expressions. Try out natural expressions today by installing a milestone release of 2201.13.0.

1. [Set up prerequisites to use natural expressions](https://ballerina.io/learn/work-with-llms-using-natural-expressions/#set-up-the-prerequisites)

2. [Configure the LLM](https://ballerina.io/learn/work-with-llms-using-natural-expressions/#configure-the-llm)

3. Set up the database, you could use [script.sql](./script.sql)

4. Add the database configuration to the Config.toml file. The Config.toml file should contain both the database configuration and the LLM configuration. For example, with the default Ballerina model, the content of your Config.toml could look like the following:

    ```toml
    [dbConfig]
    user="<DB_USER>"
    password="<DB_PASSWORD>"
    host="<DB_HOST>"
    port=<DB_PORT>
    database="<DB_NAME>"

    [ballerina.np.defaultModelConfig]
    url = "<DEFAULT_MODEL_URL>"
    accessToken = "<DEFAULT_MODEL_ACCESS_TOKEN>"
    ```

5. [Run the service](https://ballerina.io/learn/work-with-llms-using-natural-expressions/#run-the-service)

### Invoke the service

Try submitting a blog post. 

For example, add the following content in a file named blog.json.

```json
{
  "title": "An Introduction to Python: Why It's the Go-To Language for Beginners",
  "content": "Python has become one of the most popular programming languages in the world, and for good reason. Known for its simplicity and readability, Python is often recommended as the first language for new programmers. In this article, we'll explore why Python stands out and how it can be used in various fields.\n\nWhy Python?\nPython's syntax is straightforward, resembling everyday English, which reduces the learning curve for beginners. For instance, printing a simple message to the console is as easy as writing:\n\nprint(\"Hello, World!\")\n\nUnlike many other languages, Python does not require strict type declarations or complex setup processes. This makes it ideal for those just starting their programming journey.\n\nVersatile Applications\nPython is not just for beginners; it's a powerful language used in numerous domains, including:\n\nWeb Development: Frameworks like Django and Flask make building web applications easier.\nData Science and Machine Learning: Libraries like NumPy, Pandas, and TensorFlow are widely used by data scientists and AI researchers.\nAutomation: Python can automate repetitive tasks, such as file management and web scraping.\n\nThe Python Community\nAnother reason for Python's success is its supportive community. Beginners can find a wealth of tutorials, forums, and free resources online to guide them.\n\nConclusion\nPython's combination of simplicity, versatility, and community support makes it a great choice for programmers at any level. If you're looking to start your coding journey, Python is an excellent place to begin."
}
```

Then use the following cURL command to submit the blog.

```cmd
curl -X POST http://localhost:8080/blog \
  -H "Content-Type: application/json" \
  -d @blog.json
```

If the blog is accepted, the database will be updated with the blog content, along with the category and the rating.
