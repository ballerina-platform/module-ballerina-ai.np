DROP DATABASE IF EXISTS blogs;

CREATE DATABASE blogs;

USE blogs;

CREATE TABLE Blog (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    rating INT NOT NULL,       
    category VARCHAR(100) NOT NULL
);

-- INSERT INTO Blog (title, content, rating, category) VALUES ('Revolutionizing AI in Software Development',  'The integration of AI into software development tools is paving the way for smarter, faster, and more efficient coding processes. In this blog, we explore the latest AI innovations that are changing the game for developers.', 5, 'Tech Innovations & Software Development');
-- INSERT INTO Blog (title, content, rating, category) VALUES ('Top 5 Programming Languages to Learn in 2025', 'With technology evolving rapidly, it is important to stay ahead of the curve. In this post, we review the top 5 programming languages that are expected to dominate the industry in 2025.', 4, 'Programming Languages & Frameworks');
-- INSERT INTO Blog (title, content, rating, category) VALUES ('Automating Deployment with DevOps Tools', 'DevOps has transformed the way companies manage and deploy software. This blog dives into the most effective tools for automating deployment and how to integrate them into your workflow.',  5, 'DevOps, Cloud Computing & Automation');
