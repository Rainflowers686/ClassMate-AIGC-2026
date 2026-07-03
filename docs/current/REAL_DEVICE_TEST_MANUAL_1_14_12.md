# Real Device Test Manual - 1.14.12 / 125

## A. Micro-Quiz Generation

1. Install fresh build and import a new OCR/image course.
2. Generate the course and open Knowledge Timeline or Review Plan.
3. Start micro-quiz.
4. Expected: questions are based on subject knowledge points, not copied OCR sentences.
5. Expected: not all questions are true/false; correct answers are not all A.

## B. Answer Explanations

1. Submit one correct and one wrong answer.
2. Expected: answer review shows correct answer, knowledge point, evidence quote and rationale for wrong options.

## C. Completion Stability

1. Finish all quiz questions from Review Plan.
2. Expected: the app does not crash.
3. Expected: the learning record is saved and the app returns to Review Plan.
4. Repeat from Course Timeline.
5. Expected: same Review Plan return behavior.

## D. Evidence Page Note

Some evidence-detail pages may still show no micro-quiz for a single knowledge point. This is deferred in 1.14.12, but existing evidence-backed questions must not disappear.

## E. BlueLM/qwen Regression Guard

Do not reconfigure BlueLM while testing this patch. 1.14.12 keeps the 1.14.9 qwen3.5-plus mode mapping and only changes quiz generation quality and completion stability.
