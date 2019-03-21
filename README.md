---
author:
- Arnav Dhamija
title: 'RFC: Disruption Tolerant Networks in UnetStack'
---

Abstract {#abstract .unnumbered}
========

Disruption Tolerant Networks (DTNs) are employed in applications where
the network is likely to be disrupted due to environmental conditions or
certain topologies where it is impossible to find a direct route from
the sender to the receiver. DTNs are good candidates for situations in
which transmitting the message successfully is of primary importance
over message delivery times and network throughput.

Underwater networks typically use acoustic waves for transmitting data.
This suffers from interference and signal attenuation in the
environment. Certain applications of acoustic networks do not have
strict bounds on message delivery times which make DTNs ideal for these
scenarios. My undergrad thesis project is to create DTN protocols for
the UnetStack project and to test the effectiveness of these as compared
to conventional network protocols.

The link to the code can be found
[here](https://github.com/shortstheory/underwater-dtn).

Introduction
============

Unlike conventional network protocols which rely on end-to-end
connectivity, DTNs do *not* require a direct link from the source to the
destination. Messages can be routed through multiple nodes in case there
is no direct path from the source to the destination.

Some DTN routing algorithms use packet replication to send datagrams to
the destination. This approach can work when sending messages does not
require much power and there is sufficient network bandwidth. Underwater
acoustic networks are constrained on these two counts and hence,
creating copies of datagrams is sub-optimal.

Another concern in underwater networks is that certain links are only
available under certain conditions. For example, high bandwidth optical
links are short-ranged and require a Line Of Sight to the destination
for communication. Ideally, a DTN protocol should be able to switch
between different links depending on the environmental conditions.

Use Cases
---------

-   **NUSwan**: The *NUSwan* is a water surface dwelling robot which
    autonomously collects data about the water quality in Singapore’s
    reservoirs with its sensors. This data is relayed to the cloud using
    an LTE connection. However, in large reservoirs, the LTE connection
    may be temporarily unavailable due to lack of coverage. A DTN
    protocol for storing pending messages until a link is available can
    be useful in this scenario.

-   **Data Muling**: UnetStack is used on sensor nodes for collecting
    sensor measurements from parts of the ocean. The sensor stores the
    data until a scuba diver can retrieve the sensor. This is a labour
    intensive procedure. To supplant this, a DTN protocol can be used
    for an AUV which comes in close proximity of the sensor for
    downloading its data.

Goals
-----

My primary aim for this project is to develop a new [`LINK`]{} agent in
Groovy which can provide **single-hop** and **single-copy** disruption
tolerant communication. This will be called [`DtnLink`]{}. As previously
mentioned, underwater communication is power-intensive. Hence, a pending
message should only be sent when the sender is within communication
range of another node. To accomplish this, [`DtnLink`]{} will send a
probe message at a set interval to advertise its existence to nearby
nodes (a so-called “Beacon” message). On receiving this probe message, a
node can start sending datagrams to the destination.

UnetStack nodes also have the capability to *snoop* on the messages
destined for other nodes sharing the same physical medium for
communication. This capability is used for discovery of other nodes
without having to send an explicit probe message.

The [`DtnLink`]{} will also support the capability to store messages on
the internal storage of nodes until it can be sent to the destination.
It will also manage the deletion of messages whose TTL has expired.

At the time of writing, the [`DtnLink`]{} will not manage the routing of
the messages through the DTN. Instead, this will be handled by the
[`ROUTING`]{} agent.

Functionality
=============

Unet Capabilities
-----------------

[`DtnLink`]{} advertises support for the [`LINK`]{} and [`DATAGRAM`]{}
service. Along with this, [`DtnLink`]{} offers capabilities for
[`FRAGMENTATION`]{} and [`RELIABILITY`]{} (guaranteed
[`DatagramDeliveryNtf/DatagramFailureNtf`]{}).

Datagrams requested to be sent by the [`DtnLink`]{} must have a set TTL
value, or they will be refused at the outset.

Features
--------

[`DtnLink`]{} aims to be a drop-in addition to Unet containers for
adding disruption tolerant communication support. It supports the
following features.

-   **Storage**: [`DtnLink`]{} stores datagrams on the node’s internal
    storage until the datagram can be sent to the destination.

-   **TTL**: Datagrams saved to the node’s internal storage will be
    deleted when the TTL of the datagram is exceeded.

-   **Retry Limits**: [`DtnLink`]{} retries datagrams which have failed.
    This continues until the datagram reaches its retry limit or times
    out, after which it is deleted.

-   **Reliability**: [`DtnLink`]{} only uses Link agents supporting
    reliability for sending messages. Hence, we are guaranteed to know
    if a datagram has failed or has been successfully delivered. On
    message delivery, [`DtnLink`]{} passes a [`DatagramDeliveryNtf`]{}
    to the requesting application. On the other hand, if a datagram
    times out, [`DtnLink`]{} will send a [`DatagramFailureNtf`]{} to the
    application.

-   **Beacons**: As acoustic communication has high energy costs,
    datagrams should only be sent when the destination node is likely to
    receive the message. To this end, this agent periodically sends
    empty Broadcast datagrams on all its underlying Link agents. This is
    used for alerting other nodes about its presence. On receiving a
    Beacon, a node can start sending datagrams residing in its internal
    storage to that node.

-   **Multiple Links**: A particular node may have multiple available
    Link agents. [`DtnLink`]{} collects all the agents which support
    reliability and automatically switches between these agents
    depending on their availability. The priority of these links and the
    time the links to time out can also be configured.

-   **Fragmentation**: Datagrams which exceed the MTU of the underlying
    links are split into smaller segments which are sent like regular
    datagrams. The receiver waits for the reception of all of these
    segments before reassembling the original message.

-   **Randomised Timing**: While a rare issue in real-world deployments,
    datagrams sent at exactly the same time can result in collisions.
    Hence, [`DtnLink`]{} uses a [`PoissonBehavior`]{} for sending
    datagrams and beacons.

-   **Stop-And-Wait Sending**: In its current state, [`DtnLink`]{} only
    sends one datagram at a time and waits for a notification about
    whether the delivery status of the datagram before sending the next
    one.

-   **Short-circuit Sending**: As implemented by the newer Unet3 agents,
    [`DtnLink`]{} short-circuits datagrams on single-hop routes by
    sending the data without its [`DtnLink`]{} headers. This reduces the
    message size.

Configurable Options
--------------------

[`DtnLink`]{} is highly configurable and the following Parameters can be
adjusted depending to the use-case:

-   **Constants**: The [`BEACON_PERIOD`]{} (node advertising messages),
    [`SWEEP_PERIOD`]{} (deleting expired and delivered datagrams from
    internal storage), [`DATAGRAM_PERIOD`]{} (time period of sending
    datagrams), [`MAX_RETRIES`]{} (maximum number of times a datagram
    can be sent before deletion), and [`LINK_EXPIRY`]{} (time for which
    a link can remain idle without removing it from the active links
    list) constants can be set as parameters at runtime.

-   **Datagram Priority**: Datagrams can be sent according to their
    order of [`ARRIVAL`]{}, ascending order of [`EXPIRY`]{} times, and
    in a [`RANDOM`]{} manner. These options are exposed in the
    [`DATAGRAM_PRIORITY`]{} parameter.

-   **Link Priority**: The order in which underlying links are used by
    [`DtnLink`]{} can be changed by sending a list of the AgentIDs to
    [`LINK_PRIORITY`]{}.

PDU Structure
-------------

[`DtnLink`]{} encodes the data from datagrams by appending its own
10-byte header. The structure of this header, in order, is as follows:

-   24-bit TTL, representing the lifetime of the datagram in seconds.

-   8-bit Protocol number of the original datagram.

-   1-bit To Be Continued (TBC) bit, for informing the receiver if more
    datagrams are expected.

-   8-bit Payload ID, for uniquely identifying large messages by the
    tuple of their sender and the Payload ID.

-   23-bit Starting pointer, for informing the receiver about where to
    insert the contents of a fragment into its payload file.

For datagrams which do not need to be fragmented, the 32 bits following
the Protocol number are all set to 0.

Multi-hop Routing
=================

For multi-hop routing, [`DtnLink`]{} relies on the Router agent to send
the datagrams to the final destination. A sequence diagram of what this
looks like is shown below:

![Multi-Hop Message
Sending[]{data-label="mhd"}](multi-hop-delivery.png){width="0.9\linewidth"}

In such a case, [`DtnLink`]{} *will* encode its headers to preserve the
datagrams TTL information throughout the network. It may be useful to
also maintain the nodes through which the datagram has passed for future
enhancements to algorithm.

Changes Required in UnetStack
=============================

As shown in \[mhd\], the [`DatagramNtf`]{} on the intermediate node
(Node 2) first passes through the [`DtnLink`]{} for decoding. Once
decoded, the [`DtnLink`]{} emits a [`DatagramNtf`]{} for the Router
which takes the datagram, consults its routing tables, and then passes
the datagram down to [`DtnLink`]{} for sending to the next node.
However, UnetStack requires some changes to implement this behaviour:

-   [`DatagramNtf`]{} must have a TTL field for the forwarding of the
    datagram from the [`DtnLink`]{} to the Router. This TTL from the
    [`DatagramNtf`]{} should be copied to the [`DatagramReq`]{} which
    the Node 2 Router sends to its own [`DtnLink`]{}.

-   The Router should have a preference for routing
    [`DatagramNtf / DatagramReq`]{} with a set TTL value via disruption
    tolerant links.

    **NOTE:** For this to work, we need to have a way of identifying
    what a Disruption Tolerant Link is! Maybe [`DtnLink`]{} can expose
    such a Parameter and the Router can look for that?

-   Link agents should have a [`BITRATE`]{} property so the
    [`DtnLink`]{} can set the priority of its underlying Links
    accordingly.

-   [`UdpLink`]{} does not currently work in Unet3 as mentioned in this
    GitHub [issue](https://github.com/org-arl/unet-contrib/issues/17).
    This is required for testing the NUSwan simulation scenario.

Automated Regression Testing
============================

The [`DtnLink`]{} agent has several functionalities which can be tested
reproducibly. As the agent acquires new features, it is important that a
basic subset of its functionality remains intact. To this end, I have
prepared automated unit tests using [`JUnit`]{} with Groovy. These tests
check key implementation features of [`DtnLink`]{} are working
correctly.

As the [`DtnLink`]{} will work in conjunction with several other agents,
it is more useful to see the output of the agent on certain inputs
rather than diving into the internal implementation of how each function
performs. This is formally called “Black-box” testing.

![Black-box
testing[]{data-label="bb"}](blackbox.png){width="0.9\linewidth"}

The above figure is a simple example of the key concept of the
black-box. The internals can be totally abstracted for the tests as we
only wish to see the outputs of the black-box on certain inputs. In
these tests, the [`DtnLink`]{} is the black-box and the specially
developed [`TestApp`]{} and [`TestLink`]{} agents test the behaviour of
the [`DtnLink`]{}. More specifically, the [`TestApp`]{} prepares
[`DatagramReqs`]{} for sending to the [`DtnLink`]{} and the
[`TestLink`]{} checks the receipt of these datagrams, and send the
corresponding [`Ntfs`]{} to the [`DtnLink`]{}.

By these means, we can “trick” the [`DtnLink`]{} into behaving as it
would in a multi node simulation. The following tests are conducted with
this test suite:

-   [`TRIVIAL_MESSAGE`]{}: This test sends an empty [`DatagramReq`]{} to
    [`DtnLink`]{} to check if the agent correctly accepts messages with
    TTLs encoded.

-   [`SUCCESSFUL_DELIVERY`]{}: This test sends a [`DatagramReq`]{} with
    the [`USER`]{} protocol number to check if the datagram sent to the
    underlying link is formatted correctly.

-   [`ROUTER_MESSAGE`]{}: This test sends a [`DatagramReq`]{} with the
    [`ROUTING`]{} protocol number to check if the datagram sent to the
    underlying link is encoded correctly with the [`DTNL-PDU`]{} scheme
    and has its TTL adjusted accordingly.

-   [`MAX_RETRIES`]{}: This [`TestLink`]{} intentionally fails the
    datagram sent by [`DtnLink`]{} to check if the agent correctly
    attempts to resend the datagram until the message is successfully
    sent.

-   [`EXPIRY_PRIORITY`]{}: This test checks if the datagrams sent to
    [`DtnLink`]{} in [`EXPIRY_PRIORITY`]{} mode from [`TestApp`]{} are
    forwarded to the [`TestLink`]{} in order ascending order of their
    TTL values.

-   [`ARRIVAL_PRIORITY`]{}: This test checks if the datagrams sent to
    [`DtnLink`]{} in [`ARRIVAL_PRIORITY`]{} mode from [`TestApp`]{} are
    forwarded to the [`TestLink`]{} in order ascending order of their
    arrival times.

-   [`RANDOM_PRIORITY`]{}: : This test checks if the datagrams sent to
    [`DtnLink`]{} in [`RANDOM_PRIORITY`]{} mode from [`TestApp`]{} are
    forwarded to the [`TestLink`]{} in random order without regards to
    the TTL values or arrival time.

-   [`TIMEOUT`]{}: This test checks if [`DtnLink`]{} correctly disables
    sending messages on links which have not sent a message for a
    certain period of time.

-   [`MULTI_LINK`]{}: This test checks if [`DtnLink`]{} correctly uses
    the priority of Links set through a [`ParameterReq`]{} to change the
    priority of the links used to communicate with other nodes.

-   [`PAYLOAD_FRAGMENTATION`]{}: This test sends a large datagram to a
    [`DtnLink`]{} agent and checks if the datagram is successfully split
    into smaller fragments.

-   [`PAYLOAD_REASSEMBLY`]{}: This test check if the segments of a large
    datagram sent to the [`DtnLink`]{} agent can be recombined into a
    large datagram which can be passed up to the application.

Limitations
===========

[`DtnLink`]{} suffers from some design limitations which may be
revisited in the future:

-   The Stop-And-Wait sending approach of the [`DtnLink`]{} is simple
    and effective, but it can negatively impact network throughput.

-   If an underlying Link agent malfunctions and does not send *any*
    [`DDN / DFN`]{} to the [`DtnLink`]{}, the [`DtnLink`]{} *will* get
    stuck in an unrecoverable state of waiting for a notification to
    arrive before sending the next datagram.

-   In the unlikely case that a node runs out of free space on the
    internal storage when receiving a datagram enroute to another node
    via a multi-hop path, the [`DDN`]{} for that datagram will be sent
    to the immediate sender of the datagram but the datagram will not be
    saved on the node’s internal storage. The datagram will be lost in
    transit despite the sender thinking the outbound [`DDN`]{} was
    successfully delivered.

-   The TTL of a datagram is only decremented when it leaves the sending
    node. Therefore, the propagation delay is not accounted for in the
    TTL.

-   Expired and deleted datagrams are deleted en masse using a
    [`TickerBehavior`]{}, rather than deleting these datagrams as soon
    as possible. This delayed behaviour of solution can be an issue in
    nodes with limited internal storage.

-   Payload IDs are only 8-bits to conserve PDU space. However, this can
    cause the problem of two payloads having a high probability of
    sharing the same ID. This can be patched up by checking if the
    payload ID is unique on generation and trying another one if it is
    not.
