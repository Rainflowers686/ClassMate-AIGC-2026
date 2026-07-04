# PPT Content Draft - ClassMate Round Two

## 1. Cover

Title: ClassMate  
Subtitle: AIGC-powered evidence-bound learning loop app  
Competition: 2026 中国高校计算机大赛-AIGC创新赛  
Team: 待填  
Speaker notes: "ClassMate helps students turn fragmented classroom materials into structured review tasks and micro-practice."

## 2. Agenda

- Team introduction
- Product overview
- Design concept
- Prototype and effects
- Large-model application
- Innovation and value
- Submission materials

## 3. Team Introduction

Members: 待填  
Roles: product manager, UI/interaction, Android development, AI capability integration, QA/materials.  
Screenshot: team photo or avatar placeholders.

## 4. Product Overview

ClassMate is a study-loop app for college course review. Students import classroom images, text, files, and recordings. The app organizes subject knowledge points, binds evidence, prepares micro-quizzes, updates review plans from answers and feedback, and exports polished study packs.

## 5. Design Concept

Core idea: "Every AI output must be useful for review and traceable to course evidence."  
Pain point: students collect many screenshots and notes but lack a structured path from material to review.  
Approach: material basket -> knowledge extraction -> evidence binding -> practice -> feedback -> export.

## 6. Prototype and Effects

Pages to show:

- Home and import
- OCR/material tray
- Course summary
- Knowledge timeline
- Review plan
- Practice session
- Export center
- Developer diagnostics

Each page should use real screenshots from the final APK.

## 7. Interaction Flow 1

Import material -> OCR/manual correction -> course analysis -> summary/timeline -> auto-prepared micro-quiz.

## 8. Interaction Flow 2

Review plan -> practice -> answer explanation -> completion -> updated review plan.

## 9. Large-Model Application

- BlueLM/qwen3.5-plus style official cloud call, surfaced to users as BlueLM/蓝心大模型.
- Three modes:
  - Fast: low reasoning, no deep thinking.
  - Balanced: medium reasoning.
  - Professional: UI max mapped to API high, deep thinking enabled.
- Used in course analysis, summary, related knowledge, quiz generation, feedback optimization, and polished export.
- `reasoning_content` is not shown to normal users.
- Secrets are stored outside git and masked in diagnostics.

## 10. Innovation

- Multi-modal material to learning loop.
- Evidence-bound knowledge and questions.
- Automatic quiz preparation after submission.
- Quiz quality gate filters placeholder/meta/low-quality items.
- Persistent real-device diagnostics for QA.

## 11. Value and Prospects

Users: college students in review-heavy courses.  
Scenarios: after class, before exams, group note review, screenshot-heavy courses.  
Value: reduces organization cost and makes review evidence-driven.  
Potential: campus learning assistant, teacher-student review packs, integration with LMS.

## 12. Submission Materials

- PPT: this content draft.
- Poster: copy/layout draft.
- Video: 3-minute vertical script.
- APK: debug build for running demo.
- Core LLM code package: safe source package.

## 13. Closing

ClassMate demonstrates a complete AIGC learning workflow: AI helps structure, practice, verify, and export learning outcomes.
