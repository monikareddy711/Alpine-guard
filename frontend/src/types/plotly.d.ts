declare module 'plotly.js-dist-min' {
  export interface PlotlyTrace {
    type?: string;
    mode?: string;
    name?: string;
    x?: unknown[];
    y?: unknown[];
    [key: string]: unknown;
  }

  export interface PlotlyLayout {
    [key: string]: unknown;
  }

  export interface PlotlyConfig {
    [key: string]: unknown;
  }

  export interface PlotlyEvent {
    points?: Array<{ pointIndex?: number; curveNumber?: number; x?: unknown; y?: unknown }>;
  }

  const Plotly: {
    newPlot(element: HTMLElement, data: PlotlyTrace[], layout?: PlotlyLayout, config?: PlotlyConfig): Promise<unknown>;
    react(element: HTMLElement, data: PlotlyTrace[], layout?: PlotlyLayout, config?: PlotlyConfig): Promise<unknown>;
    purge(element: HTMLElement): void;
    Plots: { resize(element: HTMLElement): void };
  };
  export default Plotly;
}
