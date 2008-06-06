from ExtensionConnection import ExtensionConnection
from com.googlecode.webdriver.Navigation import Navigation

class FirefoxNavigation(Navigation):
  def __init__(self):
    self.conn = ExtensionConnection()

  def Back(self):
    self.conn.command("goBack")

  def Forward(self):
    self.conn.command("goForward")

  def To(self, url):
    self.conn.command("get", url)
