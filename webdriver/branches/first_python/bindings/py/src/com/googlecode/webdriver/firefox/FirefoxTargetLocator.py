from ExtensionConnection import ExtensionConnection
from com.googlecode.webdriver.TargetLocator import TargetLocator
import exceptions

class FirefoxTargetLocator(TargetLocator):
  def __init__(self):
    self.conn = ExtensionConnection()

  def Window(self, windowName):
    resp = self._command("switchToWindow", windowName)
    if not resp or "No window found" == resp:
      raise exceptions.InvalidSwitchToTargetException("Window %s not found" % windowName)
    self.conn.context = resp

  def FrameByIndex(self, index):
    resp = self._command("switchToFrame", str(index))

  
  def _command(self, _cmd, *args):
    return self.conn.command(_cmd, params = args)