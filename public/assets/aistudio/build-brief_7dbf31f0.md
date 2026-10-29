# GHAR — Build Brief

**A working title. Rename freely.**
A native Android budgeting app that makes UPI money feel physical again, then shows the user a parallel life where they saved it — without ever making them feel small.

*This document is written to be pasted into Google AI Studio Build mode as the seed spec. It is also a design reference for the human team.*

---

## 0. THE ONE-LINE THESIS

UPI didn't make spending cheaper. It made it **painless** — the money was never physical and the "payment" is a green tick. This app is a **pain re-coupling engine**: it gives money back its weight, then teaches saving and reinvesting through two characters who care about the user.

The product is not a tracker. It is **a home you walk into**, where a mother watches your daily spending with warmth and restraint, and an older brother quietly compounds money next to you.

---

## 1. THE TWO CHARACTERS

### 1.1 Maa (the motherly coach)

**Persona rule:** high warmth **and** high standards — the authoritative pattern, not authoritarian (cold, guilt-inducing) and not permissive (warm, no spine). A mother who believes in you and still tells you the truth.

**Her defining trait is restraint.** Her power comes from scarcity: silence *is* the reassurance. If she speaks every day she becomes a nag and the user tunes out. She must earn the right to speak.

**Hard rules — never break these:**

1. **Silence is the default state.** No insight = everything is fine. The user should read her quiet as approval.
2. **She watches patterns, not transactions.** One ₹20 chai: nothing. Four in a week: she notices. Thresholds are about repetition and drift, never a single purchase.
3. **She never says "you should have."** Ever. No past-tense blame.
4. **She speaks in questions and offers, never commands.** "You've had chai out four times this week — the office machine tomorrow?" Not "stop buying chai."
5. **She hands over exactly one doable next step.** Never a list, never a verdict.
6. **She praises.** Warmth is her baseline voice; if she never praises, the one time she speaks reads as an attack.
7. **She knows when to stay quiet.** Medical bill, rough week, big necessary expense — she does not scold. Scolding someone in a hard week loses them permanently.
8. **She never invents a number.** Every figure comes from the engine. She only wraps it in warmth.
9. **She never moralizes.** No lectures, no "as a responsible adult." One glance, one offer, done.

**Voice budget:** max **1 proactive insight per day**, and max **3 per week**, unless the user opens the app and asks. Prioritise: safety (overspend pace) > savings opportunity > praise > streak.

**Tone calibration:** the onboarding sets a warmth/strictness dial per user (see §4). A guilt-prone user gets a gentler Maa and looser thresholds; a stoic user gets a firmer one.

**Copy style:** short sentences. Indian English, warm and plain. No exclamation spam (max one "!" and only for praise). Never use the word "should."

---

### 1.2 Bhai (the elder-brother wallet)

**Persona rule:** he is not better than the user, he is *ahead* of them. He started earlier, he's been where they are, and he's building something he'll hand over. He is a sibling, never a judge.

**The reframe that must hold:** this is NOT "good person vs. evil user." Bhai is the version of the user who began a year earlier.

**Mechanics:**

1. **He speaks as "we," not "you should have."** He's on the team.
2. **He grows visibly.** His wallet (call it the **Gullak** — the traditional clay piggy bank) accumulates in real time as the user makes better choices, and shows what that money becomes over time.
3. **He has a destination.** From onboarding, the user names what they're saving for (a trip, a laptop, an emergency fund, a bike). Bhai's Gullak is saving toward *that* — so the aspiration has a target, not just a number.
4. **The gap is a winnable challenge, not a scoreboard of failure.** The app lets the user visibly *close* the distance between themselves and Bhai over time.
5. **He covers for you.** When the user slips, Bhai quietly absorbs it and the gap widens by a little — never a shame spike, never gloating.
6. **Gain framing, never loss framing.** Never "you lost ₹80,000." Always "Bhai's Gullak is at ₹80,000, and it's yours." Same math, opposite emotion.

**Reinvestment teaching (careful):** Bhai's Gullak can show a small amount routed to a realistic Indian instrument — an index SIP, a recurring deposit, PPF. Framing must be **illustrative, not advisory**: "if this pace kept up, at a typical historical rate, this is roughly the shape." Always state that past returns don't guarantee future ones, and that this is not financial advice.

---

## 2. THE HOUSE (navigation metaphor)

The "enter the house" animation is not an intro — it is the **spatial model of the whole app**. Every screen is a room. This gives visual identity, the homely feeling, and the Material 3 Expressive motion in one decision.

- **Front door → entry animation.** Onboarding ends here. The user walks in. This is the emotional handshake; make it good.
- **Kitchen — Maa.** Daily spending and her insights live here. The heart of the app.
- **Bhai's room — the Gullak.** The parallel wallet, growing. Warm, a bit of a sanctuary.
- **Study — goals & reinvest.** Where the saving destination is managed and the reinvest illustration lives.
- **Living room — month at a glance.** Overview: safe-to-spend, pace, this month's shape.
- **Balcony — the monthly report.** A calm end-of-month look back.

Rooms should be **tappable spaces**, not a bottom nav bar of icons. Optional: a small persistent "hallway" affordance for quick movement, but the primary model is spatial.

---

## 3. THE ENGINE (deterministic — no LLM in the core)

Three layers. Only the third could ever involve a model, and even then, not in v1.

### 3.1 Capture (pure code)

- **NotificationListenerService** reads UPI and bank payment notifications (GPay, PhonePe, Paytm, bank apps). This is the primary capture path and the app's biggest functional edge — and it's why a sideloaded APK is the right delivery (Play policy makes this painful for published apps).
- **SMS parser** as a secondary path: regex against known bank sender IDs and UPI/bank message formats.
- **Extraction:** amount, direction (debit/credit), counterparty/merchant, timestamp, source.
- **Dedupe:** notification + SMS often describe the same transaction; match on amount+time±window.
- **Fallback:** manual quick-add for cash, always available, never forced.
- **Permission handling:** ask gently, explain why, degrade gracefully to manual if denied.

### 3.2 Understanding (code + statistics)

- **Categorisation:** rules + a merchant lookup table + learned corrections from the user. No ML needed for a single user; a lookup table + rules covers ~90%.
- **Discretionary vs fixed:** set in onboarding; drives everything downstream.
- **Pattern detection:** rolling windows over the last 7/30 days. Frequency counts per merchant/category, trend vs. previous period, anomalies (unusually large, unusual time-of-day, unusual merchant).
- **Safe-to-spend:** `(income − fixed commitments − savings target − spent so far) / days remaining`. Recalculates live. This is the single number the whole app orbits.
- **Forecast:** burn-rate projection — "at this pace, month-end lands around ₹X."
- **Guilt-safety signals:** detect context (a large necessary/medical expense, a bad week) and suppress Maa accordingly.

### 3.3 Voice (templates, not generation)

The "smartness" is a **rules engine plus a hand-written template library**. A mother's wisdom isn't infinite — it's a handful of truths applied well. 30–50 templates *is* Maa.

Why not an LLM here: the coach must never hallucinate a number, never drift off-tone, never be sycophantic. Templates guarantee accuracy and consistent character. (An LLM can be bolted on *later* as a separate, clearly-walled "talk to Maa" chat feature — see §9.)

---

## 4. ONBOARDING

Purpose: not just to collect data, but to **calibrate the two characters**. Each answer feeds the model.

Beautiful, calm, homely. A few MCQs, not a form. Ends at the front door.

| # | Question | What it calibrates |
|---|----------|--------------------|
| 1 | Age band | Tone register; typical income cadence and categories |
| 2 | Life stage (student / salaried / self-employed / other) | Budget archetype; which patterns matter |
| 3 | Rough monthly income | The denominator for safe-to-spend |
| 4 | What's fixed vs. flexible (rent, EMIs, etc.) | What even counts as discretionary |
| 5 | Habit-type MCQ (where money leaks most: food delivery, cabs, shopping, subscriptions, chai/coffee, nights out) | Which patterns Maa watches most closely for *this* user |
| 6 | Mentality MCQ (attitude to money, impulse vs. planned) | **Guilt-sensitivity.** Sets Maa's warmth/strictness dial and thresholds |
| 7 | "What are you saving for?" | The destination for Bhai's Gullak — gives aspiration a target |
| 8 | Pain dial (optional) | How strongly the physical-cash animation should land |

**Guilt calibration is the important one.** A shame-sensitive user gets a gentler Maa and looser thresholds. This is how "strict" is kept from becoming "guilt." The onboarding decides how strict the coach is.

Sign-in is minimal and local-first (the app is client-side only). The user then enters the house.

---

## 5. MAA'S INSIGHT TEMPLATE LIBRARY

Format per entry: **trigger → text template → tone → cooldown.**

Templates use slots filled by the engine: `{thing}`, `{n}`, `{amount}`, `{category}`, `{days}`. Never hardcode a number into copy.

### 5.1 Frequency patterns (repetition, not amount)

1. **Repeated small leak** — trigger: same merchant/category ≥3× in 7 days → "You've had {thing} out {n} times this week — worth trying the {alternative}?" (offer)
2. **Daily habit** — trigger: same small spend ≥5 consecutive days → "That {thing} has become a daily thing. No judgement — want to make it a twice-a-week treat?" (question)
3. **Food delivery creep** — trigger: delivery ≥3× in 7 days → "Delivery's been doing a lot of the cooking this week. One home-cooked night to start?" (offer)
4. **Cab habit** — trigger: rideshare ≥4× in 7 days → "Lots of cabs lately. Would a metro pass pay for itself this week?" (question)
5. **Coffee/tea out** — trigger: ≥4 in 7 days → "Your {n}th coffee out this week. The office machine's not bad, honestly." (gentle)
6. **Late-night ordering** — trigger: delivery/impulse after 22:00 ≥2× in 7 days → "Late-night orders twice this week. Sleep's cheaper than delivery." (warm)

### 5.2 Drift and trend

7. **Category up vs last month** — trigger: category +25% vs previous period → "You're spending more on {category} than last month. Not a problem yet — just flagging it early." (informative)
8. **New recurring merchant** — trigger: new merchant appears ≥3× in 14 days → "There's a new regular in your spending — {merchant}. Want me to keep an eye on it?" (question)
9. **Slow upward drift** — trigger: total discretionary trending up 3 weeks running → "Spending's been quietly creeping up. Nothing scary — want to look together?" (invite)

### 5.3 Pace and safety

10. **Spending hot** — trigger: pace >1.3× usual at this point in month → "You're moving a bit faster than usual this month. Still fine — want to see where?" (reassuring)
11. **Safe-to-spend low** — trigger: safe-to-spend < 20% of daily allowance → "Today's number is tight. A light day would put you back on track." (practical)
12. **Payday spike** — trigger: first 3 days post-income spend >40% of month → "Payday always loosens things up. Want to lock a bit aside first next time?" (offer)

### 5.4 Subscription and creep

13. **Subscription stack** — trigger: ≥4 active recurring subs → "You've got {n} subscriptions running. Want to see which ones you actually used?" (question)
14. **Unused subscription** — trigger: recurring merchant, no other activity, 60+ days → "This one's been quietly renewing. Still worth it?" (question)

### 5.5 Praise and milestones (essential — this is the baseline voice)

15. **Under-budget streak** — trigger: 7 days under safe-to-spend → "A full week on track. I noticed." (praise)
16. **Category improved** — trigger: category down vs last month → "Less on {category} this month. That's not luck." (praise)
17. **First save** — trigger: first contribution to Gullak → "You just started something. It grows faster than you think." (praise)
18. **Milestone** — trigger: Gullak crosses a round number → "Bhai's Gullak just crossed ₹{amount}. That's yours." (praise)
19. **Goal progress** — trigger: 50% toward saving goal → "Halfway to {goal}. Keep going." (praise)
20. **Return after silence** — trigger: user returns after 5+ days away → "Good to have you back. No catching up needed." (welcome — explicitly no guilt)

### 5.6 Context-aware silence (suppression rules)

21. **Hard week** — trigger: large necessary/medical expense detected → suppress all non-safety insights for 7 days. (silence)
22. **Post-overspend** — trigger: user just blew a budget → do NOT scold. Bhai absorbs it. Maa stays quiet. (silence)
23. **Streak broken** — trigger: streak reset → never mention it. Offer restart, not the loss. (silence)

*(Extend to 30–50 as the app grows. Keep every one warm, brief, and past-blame-free.)*

---

## 6. THE PHYSICAL-CASH ANIMATION

The best idea in the brief — build the app around it.

- **On logging a payment:** physical bills fly from the bottom of the screen up to the merchant's card/avatar and disappear into it.
- **Scale with pain:** the animation scales with amount. A ₹40 chai sends a single note. A ₹4,000 gadget sends a fat stack the user *feels* leaving.
- **Haptics + sound:** a soft haptic thud; a paper-rustle sound. Sound off by default, opt-in.
- **Home screen physicality:** the wallet visibly **thins** across the month, so "how much is left" is something the user *sees*, not a number they read.
- **Pain dial:** from onboarding, how hard this lands is user-adjustable (subtle → punchy).

---

## 7. VISUAL DIRECTION

- **Design language:** Material 3 **Expressive** — springy, physical motion; oversized expressive type; tactile, confident colour. Not minimal, not dark-mode-first.
- **Palette — warm and homely, never fintech-cold:** terracotta, cream, warm wood tones, a deep green accent. Explicitly avoid the cold-blue/grey-card default.
- **Motion:** spring-based, overshoot allowed, entrance transitions that feel like walking through a doorway.
- **Type:** large display type for the one number that matters (safe-to-spend); humanist body for Maa's and Bhai's lines.
- **Texture:** soft shadows, rounded forms, paper/canvas feel where the receipt or Gullak appears.

---

## 8. TECH STACK & ARCHITECTURE

Aligned with AI Studio Build mode constraints (single-activity, single-module, Compose-only, client-side only).

- **Language/UI:** Kotlin + Jetpack Compose, Material 3 Expressive.
- **Architecture:** single-activity, Compose Navigation; MVVM (ViewModels + immutable UI state).
- **DI:** Hilt (or manual DI if module constraints bite).
- **Local data:** Room for the ledger and derived patterns; DataStore for prefs, calibration, and settings.
- **Async:** Coroutines + Flow throughout.
- **Capture:** NotificationListenerService + SMS receiver + manual quick-add.
- **No backend.** Everything on-device. (Optional Firebase later — see §9.)
- **Themes:** one warm theme; the "rooms" are composables, not activities.

---

## 9. LATER (explicitly out of v1)

- **"Talk to Maa" chat** — an optional, clearly-walled LLM feature (via Firebase AI Logic so no API key sits in the APK). Separate from the deterministic coach; never allowed to invent numbers.
- **Household / shared mode** — for couples or family.
- **Wear / widget** — glanceable safe-to-spend.
- **Richer reinvest tracking** — actual instrument connections.

---

## 10. GUARDRAILS (non-negotiable)

1. **No shame, ever.** No "you should have," no blame, no scoreboard of failure. Guilt is a *retention* problem, not a manners problem.
2. **No hallucinated numbers.** Every figure is engine-derived.
3. **Restart, not reckoning.** Slip-ups get forgiveness and a restart, never a reckoning. Design against the "what-the-hell effect."
4. **Privacy.** All financial data stays on-device in v1. No silent uploads.
5. **Not financial advice.** Reinvestment illustrations carry clear "illustrative, past returns ≠ future" language.
6. **Graceful degradation.** Denied permissions → manual capture, no nagging.

---

## 11. MVP SLICE — the smallest thing that proves the soul

Build this first. If this five-minute experience lands emotionally, everything else is expansion.

1. **Onboarding** — the MCQ set, ending at the front door.
2. **The house** — entry animation, and at minimum the Kitchen and Bhai's room.
3. **Maa's chai insight** — the repeated-small-leak template, fired correctly, once.
4. **Bhai's Gullak** — the parallel wallet, growing next door, with a named destination.

Everything after that — full capture, forecasting, the monthly report, reinvest illustration — is expansion.

---

## 12. FIRST PROMPT FOR AI STUDIO

> Build a native Android app in Kotlin and Jetpack Compose (Material 3 Expressive). Working title "Ghar." The app is a budgeting / expense tracker with two recurring characters and a house as its navigation metaphor.
>
> **Structure:** single-activity, Compose Navigation, MVVM, Room for the ledger, DataStore for settings, Hilt for DI, no backend. Screens are "rooms": Kitchen (daily spending + insights from "Maa"), Bhai's Room (a parallel savings wallet called the Gullak), Study (goals), Living Room (month overview).
>
> **Characters:** "Maa" is a warm, restrained coach who speaks rarely (max 1 insight/day) and only about patterns, never single purchases; she uses questions and offers, never commands, and never blames. "Bhai" is an elder-brother parallel wallet that grows as the user saves, framed aspirationally (never as loss or guilt), with a user-named savings goal.
>
> **Onboarding:** MCQ flow collecting age band, life stage, rough monthly income, fixed vs flexible costs, habit type, a mentality/guilt-sensitivity question (which sets the coach's strictness), and a savings goal.
>
> **Core engine (deterministic, no LLM):** safe-to-spend = (income − fixed − savings target − spent) / days remaining; frequency and trend pattern detection over rolling 7/30-day windows; a template-based insight library.
>
> **Signature interaction:** when a payment is logged, animated physical cash bills fly up into the merchant card, scaling with the amount, with haptic feedback.
>
> **Aesthetic:** warm and homely (terracotta, cream, wood tones), springy expressive motion, oversized type for the safe-to-spend number — explicitly not a cold fintech look.
>
> Start with this MVP slice: onboarding → the house entry animation → the Kitchen with one working pattern-based insight → Bhai's Gullak.

---

*End of brief. Iterate on copy and thresholds as you see it running — the characters will tell you when they're wrong.*
