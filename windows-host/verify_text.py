"""Verify Unicode text only in this test's own foreground entry."""
import ctypes
from ctypes import wintypes
import tkinter as tk
from server import Desktop

desktop=Desktop()
root=tk.Tk();root.title('DeskFlow text input verification');root.geometry('500x150+80+80');root.attributes('-topmost',True)
entry=tk.Entry(root,font=('Segoe UI',18));entry.pack(fill='x',padx=20,pady=40)
original=wintypes.POINT();desktop.user.GetCursorPos(ctypes.byref(original))
result=[]
desktop.user.WindowFromPoint.argtypes=[wintypes.POINT];desktop.user.WindowFromPoint.restype=wintypes.HWND
desktop.user.GetAncestor.argtypes=[wintypes.HWND,wintypes.UINT];desktop.user.GetAncestor.restype=wintypes.HWND
desktop.user.GetForegroundWindow.restype=wintypes.HWND
text='Hello नमस्ते 😀'
def finish():
    print('Test field received: '+ascii(entry.get()),flush=True)
    result.append(entry.get()==text)
    desktop.user.SetCursorPos(original.x,original.y);root.destroy()
def type_text():
    hwnd=desktop.user.GetAncestor(root.winfo_id(),2)
    if desktop.user.GetForegroundWindow()!=hwnd or root.focus_get()!=entry:
        print('Text test skipped: test entry not focused',flush=True);root.destroy();return
    desktop.input({'type':'text','text':text});root.after(350,finish)
def focus():
    root.update_idletasks()
    x=entry.winfo_rootx()+entry.winfo_width()//2;y=entry.winfo_rooty()+entry.winfo_height()//2
    hwnd=desktop.user.GetAncestor(root.winfo_id(),2)
    target=desktop.user.WindowFromPoint(wintypes.POINT(x,y))
    if desktop.user.GetAncestor(target,2)!=hwnd:
        print('Text test skipped: entry obscured',flush=True);root.destroy();return
    desktop.input({'type':'click','x':x/(desktop.user.GetSystemMetrics(0)-1),'y':y/(desktop.user.GetSystemMetrics(1)-1)})
    root.after(350,type_text)
root.after(700,focus);root.mainloop()
if result!=[True]:raise SystemExit('Native Unicode input verification failed')
print('Real Windows Unicode text entry (English, Hindi, emoji): PASS',flush=True)
