import logging
import socket
import re
import threading
import simplejson
from webdriver_common.exceptions import *

_SOCKET_TIMEOUT = 20
_DEFAULT_PORT = 7055
logger = logging.getLogger("webdriver.ExtensionConnection")

class ExtensionConnection(object):
    """This class maintains a connection to the firefox extension.

    It follows the Borg design patthern:
    http://code.activestate.com/recipes/66531/
    """
    __shared_state = {}

    def __init__(self):
        logger.debug("extension connection initiated")
        self.__dict__ = self.__shared_state

    def driver_command(self, cmd, *params):
        return self.element_command(cmd, "null", *params)

    def element_command(self, cmd, elementId, *params):
        json_dump = simplejson.dumps({"parameters": params,
                                      "context": self.context,
                                      "elementId": elementId,
                                      "commandName":cmd})
        length = len(json_dump.split("\n"))
        packet = 'Length: %d\n\n' % length
        packet += json_dump
        packet += "\n"
        logger.debug(packet)
        lock = threading.RLock()
        lock.acquire()
        self.socket.send(packet)
        if cmd == "quit" or cmd == "close":
            lock.release()
            return

        resp = ""
        while not resp.endswith("\n\n"):
            resp += self.socket.recv(1)
        resp_length = int(re.match("Length: (\d+)", resp).group(1))
        for i in range(resp_length):
            resp += self.socket.recv(1)

        lock.release()
        logger.debug(resp)
        sections = re.findall(r'{.*}', resp)
        if sections:
            json_content = sections[0];
            decoded = simplejson.loads(json_content)
            if decoded["isError"]:
                raise ErrorInResponseException(
                    decoded['response'],
                    ("Error occurred when processing\n"
                     "packet:%s\nresponse:%s" % (packet, resp)))
            self.context = decoded["context"]  #Update our context
            return decoded
        else:
            return None

    def connect(self):
        self._connect()
        self.context = "null"
        self.context = self.driver_command("findActiveDriver")["response"]

    def _connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect(("localhost", _DEFAULT_PORT))
        self.socket.settimeout(_SOCKET_TIMEOUT)

    def is_connectable(self):
        try:
            self._connect()
            return True
        except:
            return False


