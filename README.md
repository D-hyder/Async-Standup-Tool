Async Standup Tool
An AI-powered Slack integration for running asynchronous daily standups. Built with Spring Boot, PostgreSQL, and Slack’s interactive modals, it allows team members to submit updates (yesterday’s work, today’s focus, blockers) directly in Slack, stores responses in a database, and supports automatic summarization for team leads using AI.

Features
/standup Slack slash command to trigger a modal form
Collects and stores standup responses in PostgreSQL
AI-generated summaries of team updates (future feature)
REST API for retrieving standup data
Ngrok tunneling for local development with Slack events
Modular, extensible backend design for new features

Tech Stack
Java 21 + Spring Boot
PostgreSQL
Slack Bolt / Slack Web API via Spring WebClient
Ngrok for local webhook testing
Maven for build & dependency management
