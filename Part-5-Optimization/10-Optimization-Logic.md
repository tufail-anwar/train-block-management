# Optimization Logic

## Inputs
Maintenance tasks, duration, priority/criticality, corridor, asset, allowed time window, train timetable, corridor availability, existing blocks, resources and dependencies.

## Hard constraints
- No prohibited train overlap
- No unavailable corridor window
- Required task duration must fit
- Dependencies must be satisfied
- Resource capacity must not be exceeded
- Configured operational/safety rules must be respected

## Objectives
Minimize:
- Train disruption
- Maintenance delay
- Unnecessary blocks
- Idle/resource time

Maximize:
- Critical task completion
- Block utilization
- Compatible task coordination
- Asset availability

Prototype option: Constraint Programming / Integer Programming using OR-Tools.

An LLM should not be the core scheduler; use deterministic optimization for schedule calculation.
