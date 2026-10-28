# Assessment workspace interface

## Design contract

The question bank is for finding and reusing questions; creating a question is its primary action. The builder separates selected questions and their point/order controls from available questions. The live workspace prioritizes joining and session control, then participation and results. The participant screen prioritizes the current question and a clear answer selection.

Reference evidence: the existing question-bank, assessment-builder and live-session screens were reviewed. No external catalogue was used. Preserve the product's blue identity with a restrained navy navigation and live stage, light work surfaces, fine borders and clear typography. Use consistent 4px-based spacing, 44px controls, explicit labels and calm status colours. Avoid decorative gradients, glowing effects, dense action clusters and generic hero sections.

At 320px, panels stack, actions wrap and navigation is expandable. At 768px, shared controls retain their labels and spacing. At 1024px, navigation is persistent but secondary panels stack. At 1440px and above, selected questions and available questions can share a two-column workspace. Dark participant content must keep its own high-contrast typography.

Preserve loading, empty, error, disabled, selected and finished states. Do not represent simulated analytics or fake system health. All session statistics come from the API. Verify keyboard navigation, mobile menu, answer selection, a complete live session and CSV export against the real local API.
