"""A tiny kit for hand-laid UML-style diagrams in SVG: auto-sized class boxes, panels and elbow connectors."""
import html

INK = "#1B2B45"
FONT = "Inter, Helvetica, Arial, sans-serif"
HEAD = {"aggregate root": "#DBEAFE", "value object": "#E3F4EA", "enumeration": "#EEF1F5", "domain event": "#D9F4EF",
        "read model": "#FDEFC3", "domain service": "#EFE3FB", "component": "#DBEAFE", "table": "#E8EEF6", "copy": "#FDEFC3", "": "#FFFFFF"}

def tw(text, size=12, bold=False):
    """Rough text width for Inter."""
    return len(text) * size * (0.56 if bold else 0.53)

class Box:
    def __init__(self, key, x, y, stereotype, name, attrs=(), ops=(), min_w=0):
        self.key, self.x, self.y = key, x, y
        self.st, self.name, self.attrs, self.ops = stereotype, name, list(attrs), list(ops)
        widths = [tw(name, 13.5, True) + 28, tw("«" + stereotype + "»", 10.5) + 28] + [tw(t) + 22 for t in self.attrs + self.ops]
        self.w = max(max(widths), min_w)
        self.head_h = 40 if stereotype else 30
        self.h = self.head_h + (len(self.attrs) * 17 + 12 if self.attrs else 0) + (len(self.ops) * 17 + 12 if self.ops else 0)

    def anchor(self, side, frac=0.5):
        if side == "t": return (self.x + self.w * frac, self.y)
        if side == "b": return (self.x + self.w * frac, self.y + self.h)
        if side == "l": return (self.x, self.y + self.h * frac)
        if side == "r": return (self.x + self.w, self.y + self.h * frac)
        raise ValueError(side)

    def svg(self):
        x, y, w, h = self.x, self.y, self.w, self.h
        out = [f'<rect x="{x}" y="{y}" width="{w:.0f}" height="{h}" fill="#FFFFFF" stroke="{INK}" stroke-width="1.2"/>',
               f'<rect x="{x}" y="{y}" width="{w:.0f}" height="{self.head_h}" fill="{HEAD[self.st]}" stroke="{INK}" stroke-width="1.2"/>']
        cx = x + w / 2
        if self.st:
            out.append(f'<text x="{cx:.0f}" y="{y + 15}" text-anchor="middle" font-size="10.5" fill="#33425C">«{self.st}»</text>')
            out.append(f'<text x="{cx:.0f}" y="{y + 32}" text-anchor="middle" font-size="13.5" font-weight="700">{html.escape(self.name)}</text>')
        else:
            out.append(f'<text x="{cx:.0f}" y="{y + 20}" text-anchor="middle" font-size="13.5" font-weight="700">{html.escape(self.name)}</text>')
        ty = y + self.head_h
        for group in (self.attrs, self.ops):
            if not group: continue
            if group is self.ops and self.attrs:
                out.append(f'<line x1="{x}" y1="{ty}" x2="{x + w:.0f}" y2="{ty}" stroke="{INK}" stroke-width="1"/>')
            for i, line in enumerate(group):
                out.append(f'<text x="{x + 10}" y="{ty + 20 + i * 17}" font-size="12">{html.escape(line)}</text>')
            ty += len(group) * 17 + 12
        return "\n".join(out)

def panel(x, y, w, h, title, subtitle, fill="#F7F9FC", tx=16):
    """A titled group. tx is how far the title sits from the left edge."""
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="{fill}" stroke="#B8C4D6" stroke-width="1.2"/>'
            f'<text x="{x + tx}" y="{y + 26}" font-size="15.5" font-weight="700">{html.escape(title)}</text>'
            f'<text x="{x + tx}" y="{y + 44}" font-size="11.5" fill="#475569">{html.escape(subtitle)}</text>')

DEFS = f'''<defs>
<marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M1 1 L9 5 L1 9" fill="none" stroke="{INK}" stroke-width="1.4"/></marker>
<marker id="arrowT" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M1 1 L9 5 L1 9" fill="none" stroke="#0E7490" stroke-width="1.4"/></marker>
<marker id="arrowG" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M1 1 L9 5 L1 9" fill="none" stroke="#64748B" stroke-width="1.4"/></marker>
<marker id="diamond" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="15" markerHeight="10" orient="auto"><path d="M1 5 L8 1 L15 5 L8 9 Z" fill="{INK}" stroke="{INK}"/></marker>
<marker id="odiamond" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="15" markerHeight="10" orient="auto"><path d="M1 5 L8 1 L15 5 L8 9 Z" fill="#FFFFFF" stroke="{INK}"/></marker>
<marker id="solid" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M1 1 L9 5 L1 9 Z" fill="{INK}" stroke="{INK}"/></marker>
<marker id="solidT" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M1 1 L9 5 L1 9 Z" fill="#0E7490" stroke="#0E7490"/></marker>
</defs>'''

STYLE = {
    "compose": f'stroke="{INK}" stroke-width="1.3" marker-start="url(#diamond)"',
    "aggregate": f'stroke="{INK}" stroke-width="1.3" marker-start="url(#odiamond)"',
    "uses": f'stroke="{INK}" stroke-width="1.3" marker-end="url(#arrow)"',
    "byid": 'stroke="#64748B" stroke-width="1.3" stroke-dasharray="6 4" marker-end="url(#arrowG)"',
    "event": 'stroke="#0E7490" stroke-width="1.4" stroke-dasharray="6 4" marker-end="url(#arrowT)"',
    "call": f'stroke="{INK}" stroke-width="1.6" marker-end="url(#solid)"',
    "flow": 'stroke="#0E7490" stroke-width="1.6" marker-end="url(#solidT)"',
    "line": f'stroke="{INK}" stroke-width="1.3"',
}
LABEL_FILL = {"event": "#0E7490", "flow": "#0E7490", "byid": "#475569"}

def path(points, kind, label=None, label_at=None, anchor="middle", bg="#FFFFFF"):
    d = "M " + " L ".join(f"{x:.0f} {y:.0f}" for x, y in points)
    out = [f'<path d="{d}" fill="none" {STYLE[kind]}/>']
    if label:
        if label_at is None:
            i = max(range(len(points) - 1), key=lambda k: abs(points[k + 1][0] - points[k][0]) + abs(points[k + 1][1] - points[k][1]))
            label_at = ((points[i][0] + points[i + 1][0]) / 2, (points[i][1] + points[i + 1][1]) / 2 - 5)
        lx, ly = label_at
        w = tw(label, 11) + 8
        x0 = lx - w / 2 if anchor == "middle" else (lx - 3 if anchor == "start" else lx - w + 3)
        out.append(f'<rect x="{x0:.0f}" y="{ly - 11:.0f}" width="{w:.0f}" height="15" fill="{bg}" opacity="0.92"/>')
        out.append(f'<text x="{lx:.0f}" y="{ly:.0f}" text-anchor="{anchor}" font-size="11" fill="{LABEL_FILL.get(kind, "#33425C")}">{html.escape(label)}</text>')
    return "\n".join(out)

def elbow(a, sa, b, sb, mid=None, fa=0.5, fb=0.5):
    """Points for an orthogonal connector between two boxes."""
    p, q = a.anchor(sa, fa), b.anchor(sb, fb)
    if sa in "lr" and sb in "lr":
        mx = mid if mid is not None else (p[0] + q[0]) / 2
        return [p, (mx, p[1]), (mx, q[1]), q]
    if sa in "tb" and sb in "tb":
        my = mid if mid is not None else (p[1] + q[1]) / 2
        return [p, (p[0], my), (q[0], my), q]
    if sa in "lr":   # horizontal then vertical
        return [p, (q[0], p[1]), q]
    return [p, (p[0], q[1]), q]

def document(width, height, body, title, origin=(0, 0)):
    ox, oy = origin
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{ox} {oy} {width} {height}" width="{width}" height="{height}" '
            f'font-family="{FONT}" fill="{INK}" role="img" aria-label="{html.escape(title)}">\n'
            f'<rect x="{ox}" y="{oy}" width="{width}" height="{height}" fill="#FFFFFF"/>\n{DEFS}\n' + "\n".join(body) + "\n</svg>\n")
