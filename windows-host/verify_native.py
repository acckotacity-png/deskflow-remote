"""Checks native clicks only against this test's own foreground button."""
import ctypes
from ctypes import wintypes
import io
import tkinter as tk
from PIL import Image
from server import Desktop

def main():
    desktop = Desktop()
    image = Image.open(io.BytesIO(desktop.frame()))
    assert image.width > 0 and image.height > 0
    print('Real Windows frame capture: PASS', flush=True)
    root = tk.Tk()
    root.title('DeskFlow native input verification')
    root.geometry('420x180+60+60')
    root.attributes('-topmost', True)
    clicked = []
    button = tk.Button(root, text='DeskFlow test target', command=lambda: clicked.append(True))
    button.pack(expand=True, fill='both', padx=30, pady=30)
    original = wintypes.POINT()
    desktop.user.GetCursorPos(ctypes.byref(original))
    outcome = []
    desktop.user.GetAncestor.argtypes = [wintypes.HWND, wintypes.UINT]
    desktop.user.GetAncestor.restype = wintypes.HWND
    desktop.user.GetForegroundWindow.restype = wintypes.HWND
    def verify():
        outcome.append(len(clicked) == 1)
        desktop.user.SetCursorPos(original.x, original.y)
        root.destroy()
    def click():
        root.update_idletasks()
        hwnd = desktop.user.GetAncestor(root.winfo_id(), 2)
        x = button.winfo_rootx() + button.winfo_width() / 2
        y = button.winfo_rooty() + button.winfo_height() / 2
        desktop.user.WindowFromPoint.argtypes = [wintypes.POINT]
        desktop.user.WindowFromPoint.restype = wintypes.HWND
        target = desktop.user.WindowFromPoint(wintypes.POINT(round(x), round(y)))
        if desktop.user.GetAncestor(target, 2) != hwnd:
            print('Input test skipped: test target is obscured', flush=True)
            root.destroy()
            return
        desktop.input({'type': 'click', 'x': x / (desktop.user.GetSystemMetrics(0) - 1), 'y': y / (desktop.user.GetSystemMetrics(1) - 1)})
        root.after(300, verify)
    root.after(100, root.focus_force)
    root.after(800, click)
    root.mainloop()
    if outcome != [True]: raise SystemExit('Native input verification did not pass')
    print('Actual Windows click on test-owned button: PASS', flush=True)

if __name__ == '__main__': main()
