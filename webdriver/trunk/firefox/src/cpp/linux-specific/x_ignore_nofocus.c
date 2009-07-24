#include <stdio.h>
#include <X11/Xlib.h>
#include <X11/X.h>
#include <dlfcn.h>

#define TRUE 1
#define FALSE 0

// This boolean is for remembering if we already had a FocusIn event and
// never re-send that event as well, not to break clients which expect to get
// FocusOut before FocusIn

int g_encountered_focus_in_event = FALSE;

void identify_focus_in_event(XEvent* ev)
{
  if (ev->type == FocusIn) {
    g_encountered_focus_in_event = TRUE;
  }
}

int is_focus_out(XEvent* ev)
{
  return (ev->type == FocusOut);
}

int should_ignore_focus_in(XEvent* ev)
{
  if ((ev->type == FocusIn) && (g_encountered_focus_in_event == TRUE)) {
    return TRUE;
  }
  return FALSE;
}

int should_discard_event(XEvent *ev) {
  int ret_val = FALSE;
  if ((is_focus_out(ev)) || (should_ignore_focus_in(ev))) {
    ret_val = TRUE;
  }
  // Remember, for next time, if we encountered a focus in event
  // and do not relay it.
  identify_focus_in_event(ev);
  return ret_val;
}

void fake_visibility_event(XEvent* outEvent, XEvent* sourceEvent)
{
  XEvent ev;
  ev.type = VisibilityNotify;
  ev.xvisibility.serial = sourceEvent->xfocus.serial;
  ev.xvisibility.send_event = sourceEvent->xfocus.send_event;
  ev.xvisibility.display = sourceEvent->xfocus.display;
  ev.xvisibility.window = sourceEvent->xfocus.window;
  ev.xvisibility.state = VisibilityUnobscured;
  *outEvent = ev;
}

int XNextEvent(Display *display, XEvent *outEvent) {
  // Code to pull the real function handle from X11 library.
  void *handle = NULL;
  //This will turn the function proto into a function pointer declaration
  int (*real_func)(Display *display, XEvent *outEvent) = NULL;
  const char library[] = "/usr/lib/libX11.so";
  handle = dlopen(library, RTLD_LAZY);

  if (handle == NULL) {
    fprintf(stderr, "Failed to dlopen %s\n", library);
    fprintf(stderr, "dlerror says: %s\n", dlerror());
    return -1;
  }

  // The real event from XNextEvent
  XEvent realEvent;

  // Find the real function.
  real_func = dlsym(handle, "XNextEvent");
  // Invoke the real function.
  int rf_ret = real_func(display, &realEvent);

  if (should_discard_event(&realEvent))
  {
    // Fake an event!
    fake_visibility_event(outEvent, &realEvent);
  } else {
    *outEvent = realEvent;
  }
  return rf_ret;
}