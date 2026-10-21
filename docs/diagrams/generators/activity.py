"""Generates UML activity diagrams with swimlanes as Graphviz DOT."""
import html, textwrap

FONT = "Inter"
LANE_FILL = {"actor": "#EEF4FF", "service": "#F5F7FA", "other": "#F0FAF7", "llm": "#FFF7E6"}
LANE_EDGE = "#B8C4D6"
INK = "#1B2B45"

def wrap(text, width=24):
    return "\\n".join(textwrap.wrap(text, width, break_long_words=False)) if "\\n" not in text else text

class Activity:
    def __init__(self, name, title):
        self.name, self.title = name, title
        self.lanes = []          # (key, label, kind)
        self.nodes = {}          # key -> (lane, kind, label)
        self.edges = []          # (a, b, label, style)

    def lane(self, key, label, kind="service"):
        self.lanes.append((key, label, kind)); return self

    def node(self, key, lane, kind, label=""):
        self.nodes[key] = (lane, kind, label); return self

    # shorthands
    def start(self, key, lane): return self.node(key, lane, "start")
    def end(self, key, lane): return self.node(key, lane, "end")
    def act(self, key, lane, label): return self.node(key, lane, "action", label)
    def dec(self, key, lane, label): return self.node(key, lane, "decision", label)
    def send(self, key, lane, label): return self.node(key, lane, "send", label)
    def recv(self, key, lane, label): return self.node(key, lane, "recv", label)
    def store(self, key, lane, label): return self.node(key, lane, "store", label)
    def bar(self, key, lane): return self.node(key, lane, "bar")

    def flow(self, *keys, label="", style="solid"):
        for a, b in zip(keys, keys[1:]):
            self.edges.append((a, b, label, style)); label = ""
        return self

    def to(self, a, b, label="", style="solid"):
        self.edges.append((a, b, label, style)); return self

    def node_dot(self, key):
        lane, kind, label = self.nodes[key]
        if kind == "start":
            return f'{key} [shape=circle, style=filled, fillcolor="{INK}", color="{INK}", label="", width=0.24, height=0.24, fixedsize=true];'
        if kind == "end":
            return f'{key} [shape=doublecircle, style=filled, fillcolor="{INK}", color="{INK}", label="", width=0.2, height=0.2, fixedsize=true];'
        if kind == "bar":
            return f'{key} [shape=box, style=filled, fillcolor="{INK}", color="{INK}", label="", width=1.6, height=0.07, fixedsize=true];'
        if kind == "decision":
            return f'{key} [shape=diamond, style=filled, fillcolor="#FFFFFF", color="{INK}", label="{wrap(label, 18)}", fontsize=10, margin="0.16,0.10"];'
        if kind == "send":
            return f'{key} [shape=cds, style=filled, fillcolor="#DFF7F2", color="#0E7490", label="{wrap(label, 22)}", fontsize=10.5, margin="0.18,0.08"];'
        if kind == "recv":
            return f'{key} [shape=box, style="filled,rounded,dashed", fillcolor="#DFF7F2", color="#0E7490", label="{wrap(label, 22)}", fontsize=10.5, margin="0.14,0.08"];'
        if kind == "store":
            return f'{key} [shape=cylinder, style=filled, fillcolor="#FFFFFF", color="{INK}", label="{wrap(label, 18)}", fontsize=10, margin="0.08,0.06"];'
        return f'{key} [shape=box, style="filled,rounded", fillcolor="#FFFFFF", color="{INK}", label="{wrap(label)}", fontsize=10.5, margin="0.14,0.08"];'

    def dot(self):
        out = [f'digraph "{self.name}" {{',
               f'  graph [rankdir=TB, fontname="{FONT}", fontsize=13, labelloc=t, labeljust=l, label=<<b>{html.escape(self.title)}</b>>, pad=0.25, nodesep=0.35, ranksep=0.42, splines=spline, bgcolor="white", newrank=true, fontcolor="{INK}"];',
               f'  node [fontname="{FONT}", fontcolor="{INK}", penwidth=1.1];',
               f'  edge [fontname="{FONT}", fontsize=9.5, color="{INK}", fontcolor="#33425C", arrowsize=0.7, penwidth=1.0];']
        for key, label, kind in self.lanes:
            out.append(f'  subgraph cluster_{key} {{')
            out.append(f'    label=<<b>{html.escape(label)}</b>>; labelloc=t; labeljust=c; fontsize=11.5; fontname="{FONT}"; fontcolor="{INK}";')
            out.append(f'    style="filled"; fillcolor="{LANE_FILL[kind]}"; color="{LANE_EDGE}"; margin=14;')
            out.append(f'    {key}__top [shape=point, style=invis, width=0, height=0, label=""];')
            out.append(f'    {key}__bot [shape=point, style=invis, width=0, height=0, label=""];')
            for nk, (lane, _, _) in self.nodes.items():
                if lane == key:
                    out.append("    " + self.node_dot(nk))
            out.append("  }")
        # Invisible anchors make every lane span the full height, in the declared left-to-right order.
        tops = [f"{k}__top" for k, _, _ in self.lanes]
        bots = [f"{k}__bot" for k, _, _ in self.lanes]
        out.append("  { rank=min; " + "; ".join(tops) + "; }")
        out.append("  { rank=max; " + "; ".join(bots) + "; }")
        if len(tops) > 1:
            out.append("  " + " -> ".join(tops) + " [style=invis];")
        for top, bot in zip(tops, bots):
            out.append(f"  {top} -> {bot} [style=invis, weight=0];")
        for a, b, label, style in self.edges:
            attrs = []
            if label: attrs.append(f'label=" {label} "')
            if style == "dashed": attrs.append('style=dashed, color="#0E7490", arrowhead=open')
            out.append(f'  {a} -> {b}' + (f' [{", ".join(attrs)}]' if attrs else "") + ";")
        out.append("}")
        return "\n".join(out)
