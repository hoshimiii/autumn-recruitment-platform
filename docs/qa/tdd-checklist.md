# TDD Checklist

- Write a failing unit or integration test before implementing production behavior.
- Run the targeted test and confirm the expected failure is caused by missing or incorrect behavior.
- Implement the smallest production change that satisfies the test.
- Re-run the targeted test and confirm it passes.
- Add edge cases for source policy, parsing confidence, review actions, and persistence where behavior can regress.
- Run package-level tests before committing a production task.
- Keep tests focused on public contracts rather than implementation details.
- For documentation and root config tasks, run the required placeholder scan instead of production-code TDD.
