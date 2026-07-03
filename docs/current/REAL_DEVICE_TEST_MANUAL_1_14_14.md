# Real Device Test Manual - 1.14.14 / 127

## A. Fresh Material And Auto Quiz Preparation

1. Install or open the app.
2. Add a new image/file/manual material and confirm course generation.
3. Open Review.

Expected:
- Review shows quiz preparation status.
- After analysis completes, it shows prepared quiz count or a clear insufficient-material message.
- Starting practice should not trigger the first generation from scratch if prepared questions already exist.

## B. Quiz Quality

1. Start micro-practice from Review or course timeline.
2. Inspect stems, options, fill-in answers, and explanations.

Expected:
- Questions are about subject knowledge points.
- Options are subject statements, not meta text such as OCR, original text, fallback, provider, relevance, or raw ids.
- Low-quality material shows an honest empty/insufficient state instead of bad questions.

## C. Completion Stability

1. Complete a mixed practice session.
2. Tap complete once, then repeat with a rapid double tap.

Expected:
- No crash.
- Learning record is saved.
- The app returns to the Review tab/root review plan.
- It does not exit the app or pop to an empty stack.

## D. Practice Back Arrow

1. Enter a practice session from Review.
2. Tap the top-left arrow.
3. Repeat from a course timeline entry and with the Android system back key.

Expected:
- The app returns to the Review tab/root review plan.
- Bottom navigation highlights Review.
- The app does not exit.

## E. Deferred Item

Evidence-detail pages with no quiz for some individual knowledge points are not forced in this release. Confirm existing evidence-backed questions are not removed.

## F. Provider Smoke

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\provider_live_smoke.ps1
```

Expected:
- Missing local credentials print SKIP.
- No AppKey, Authorization value, Bearer token, or raw secret is printed.
