from concurrent.futures import Future

from opendsb.routing.remote.remotepeer import RemotePeer


class RemotePeerConnection:
    def __init__(self, remote_peer: RemotePeer):
        self.remote_peer = remote_peer

    def when_connected(self) -> Future:
        connected_future = Future()
        if self.remote_peer is not None and self.remote_peer.is_bus_connected():
            connected_future.set_result(None)
        else:
            self.remote_peer.add_connected_future(connected_future)
        return connected_future

    def when_disconnected(self) -> Future:
        disconnected_future = Future()
        if self.remote_peer is None or not self.remote_peer.is_bus_connected():
            disconnected_future.set_result(None)
        else:
            self.remote_peer.add_disconnected_future(disconnected_future)
        return disconnected_future
