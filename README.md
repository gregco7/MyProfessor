# My Professor - An AI Learning Tool

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=black)

A CLI first Web-App tool designed to use LLMs to help you learn
on the edge of your current understanding.

## Setup

You need Java 25, Node, and a local PostgreSQL.

```bash
# 1. Database
createdb myprofessor

# 2. Secrets
cp .env.example .env        # fill in CLAUDE_KEY and DB_PASSWORD,
                            # and add: spring.datasource.username=<your postgres role>

# 3. Dashboard bundle
cd dashboard && npm install && npm run build && cd ..

# 4. Run
./ailearn                   # CLI + web app in one process
```

Migrations run on first start. In the shell:

```
/init_learn 'topic' 'intended proficiency' (1-4)    # proficiency: 1: aware | 2: familiar | 3: competent | 4:fluent
/sessions
```


