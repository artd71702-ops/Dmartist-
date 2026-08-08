package com.example.data.model

data class PromptTemplate(
    val id: String,
    val title: String,
    val description: String,
    val templateText: String,
    val category: String,
    val iconName: String
)

object PromptTemplateLibrary {
    val categories = listOf(
        "All",
        "Summarization",
        "Coding",
        "Websites",
        "Translation",
        "Image Gen",
        "Writing & Email",
        "Analysis"
    )

    val templates = listOf(
        PromptTemplate(
            id = "web_1",
            title = "AI Website Generator Prompt",
            description = "Generate a full-featured, responsive HTML/CSS/JS website preview with custom layout & styling.",
            templateText = "Build a responsive website for [Business or Project Name].\n\nPurpose: [e.g. Portfolio, Landing Page, E-Commerce, SaaS]\nTarget Audience: [Who is visiting?]\nPages / Sections needed: Hero, About, Services, Pricing, Testimonials, Contact\nStyle & Tone: Modern, clean, vibrant accent colors, high-contrast typography\nFunctionality: Interactive contact form, navigation bar, responsive layout\n\nOutput: Return clean single-file HTML code wrapped in ```html ... ``` block.",
            category = "Websites",
            iconName = "Language"
        ),
        PromptTemplate(
            id = "sum_1",
            title = "Executive Text Summary",
            description = "Summarize lengthy articles, reports, or notes into 3-5 concise bullet points.",
            templateText = "Summarize the following text into 3 to 5 clear, bulleted executive key takeaways:\n\n[Paste your article, document, or notes here]",
            category = "Summarization",
            iconName = "Summarize"
        ),
        PromptTemplate(
            id = "sum_2",
            title = "TL;DR One-Liner",
            description = "Condense complex text into a single easy-to-understand sentence.",
            templateText = "Provide a 1-sentence TL;DR summary for the following text:\n\n[Paste text here]",
            category = "Summarization",
            iconName = "ShortText"
        ),
        PromptTemplate(
            id = "code_1",
            title = "Explain Code Logic",
            description = "Break down complex source code line-by-line with plain language explanations.",
            templateText = "Explain how the following code works step-by-step. Highlight key data structures, algorithms, and logical flow:\n\n```\n// Paste your code here\n```",
            category = "Coding",
            iconName = "Code"
        ),
        PromptTemplate(
            id = "code_2",
            title = "Debug & Fix Code Errors",
            description = "Identify syntax errors, logical bugs, and edge-case failures with corrected code.",
            templateText = "Analyze the following code snippet for potential bugs, memory leaks, or error handling issues. Provide the fixed code along with explanations:\n\n```\n// Paste code or error stack trace here\n```",
            category = "Coding",
            iconName = "BugReport"
        ),
        PromptTemplate(
            id = "code_3",
            title = "Refactor & Clean Code",
            description = "Optimize code structure, improve readability, and adhere to best design patterns.",
            templateText = "Refactor the following code to improve readability, maintainability, and efficiency while preserving core functionality:\n\n```\n// Paste code here\n```",
            category = "Coding",
            iconName = "AutoFixHigh"
        ),
        PromptTemplate(
            id = "trans_1",
            title = "Multi-Lingual Translation",
            description = "Translate text accurately into Kinyarwanda, French, Spanish, Swahili, Chinese, etc.",
            templateText = "Translate the following text into [Specify Language, e.g. Kinyarwanda / French / Kiswahili]. Preserve formatting and tone:\n\n[Paste text here]",
            category = "Translation",
            iconName = "Translate"
        ),
        PromptTemplate(
            id = "img_1",
            title = "Imagen Photorealistic Scene",
            description = "Generate high-detail photographic artwork prompt for Imagen.",
            templateText = "Generate an image of a serene futuristic mountain sanctuary at sunrise with glowing holographic waterfalls, highly detailed, photorealistic 8k studio lighting.",
            category = "Image Gen",
            iconName = "Palette"
        ),
        PromptTemplate(
            id = "img_2",
            title = "Imagen Sci-Fi Cyberpunk Art",
            description = "Vivid neon digital art prompt for Imagen.",
            templateText = "Generate an image of a cyberpunk detective walking through a neon-lit rain-slicked city alleyway at night, vivid colors, cinematic depth of field.",
            category = "Image Gen",
            iconName = "Image"
        ),
        PromptTemplate(
            id = "write_1",
            title = "Draft Professional Email",
            description = "Compose a polite, structured email response for business communications.",
            templateText = "Draft a professional email regarding the following context:\n- Recipient: [e.g. Client / Manager]\n- Objective: [e.g. Project status update / Meeting reschedule]\n- Key Points: [Point 1, Point 2]",
            category = "Writing & Email",
            iconName = "Email"
        ),
        PromptTemplate(
            id = "write_2",
            title = "Brainstorm Creative Ideas",
            description = "Generate 10 innovative concepts for projects, products, or marketing campaigns.",
            templateText = "Brainstorm 10 creative, unique, and actionable ideas for:\n\n[Topic or Product Concept]",
            category = "Writing & Email",
            iconName = "Lightbulb"
        ),
        PromptTemplate(
            id = "ana_1",
            title = "Data & Logic Analysis",
            description = "Analyze arguments, pros/cons, and decision factors for any topic.",
            templateText = "Provide a balanced pros and cons analysis for the following decision or topic, followed by a final recommendation:\n\n[Topic or Decision]",
            category = "Analysis",
            iconName = "Analytics"
        )
    )
}
