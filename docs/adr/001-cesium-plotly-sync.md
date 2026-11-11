# 001 Synchronizing Plotly and CesiumJS

## Context
Hovering the elevation profile, clicking a station row and selecting a tour must all move the 3D camera.

## Decision
One `DashboardStore` publishes `selectedStation$`, `selectedRoute$`, `hoveredPoint$` and a `focus$`
stream of camera targets. Chart and table handlers push into the store; the Cesium viewer subscribes
to `focus$` and starts the flight inside `NgZone.runOutsideAngular`.

## Consequence
Every panel reacts in the same change detection tick, which keeps propagation inside the 100 ms budget
and lets the interaction be tested without a browser renderer. The store must stay free of panel state.
