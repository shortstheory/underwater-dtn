Abstract
---------

Disruption Tolerant Networks (DTNs) are employed in applications where the network is likely to be disrupted due to environmental conditions or where the network topology makes it impossible to find a direct route from the sender to the receiver. Underwater networks typically use acoustic waves for transmitting data. However, these waves are susceptible to interference from sources of noise such as the wake from ships, sounds from snapping shrimp, and collisions from acoustic waves generated by other nodes.

DTNs are good candidates for situations where successfully delivering the message is more important than low delivery times and high network throughput. This is true for certain applications of underwater networks. DTNs can also create new options for network topologies, such as opening up the possibility of using data muling nodes if the network is resilient to delays.

The Acoustic Research Laboratory (ARL) at NUS has developed their own Groovy-based underwater network simulator called UnetStack, in which network protocols can be designed and tested in a simulator. These protocols can later be directly deployed on physical hardware, such as Subnero's underwater modems. Hence, this project revolves around creating a new UnetStack protocol called `DtnLink` for enabling disruption tolerant networking in various use cases of the ARL.

Introduction
---------
Unlike conventional network protocols which rely on end-to-end
connectivity at a given instant of time, DTNs do *not* require a
complete path from the source to the destination when transmitting the
message.

To accomplish this, all types of DTN protocols employ a type of
Store-Carry-And-Forward (SCAF) mechanism to store the message until it
can be sent to the destination. The message can either be sent directly
to the destination or via another node in multi-hop routing.

Some DTN routing algorithms use packet replication to send datagrams to
the destination. This approach might be sub-optimal for underwater
networks which are constrained by transmission power limitations and
suffer from packet collisions when the network is flooded with messages.
Therefore, an implementation of DTNs for UnetStack MAY NOT use packet
replication.

The DTN protocol developed in this project is referred to as `DtnLink`.

Use Cases
---------

-   **Data Muling**: UnetStack is used on sensor nodes for collecting
    sensor measurements from parts of the ocean. The sensor stores the
    data until a diver can retrieve the sensor. This is a labour
    intensive procedure. To supplant this, `DtnLink` can be used for
    sending the sensor’s data to an AUV when it comes in range of the
    sensor.

-   **Time Varying Links**: A concern in underwater networks is that
    certain links are only available under certain conditions. For
    example, high bandwidth optical links are short-ranged and require a
    Line of Sight to the destination for communication. Ideally,
    `DtnLink` should be able to choose the most optimal link
    depending on the link’s availability and bitrate.

-   **USB Link**: `DtnLink` will maintain a list of pending messages
    in the node’s non-volatile storage. As a potential alternative to
    sending these messages wirelessly, a USB Link agent could work in
    conjunction with `DtnLink` for automatically copying these
    messages to an external storage device.

-   **NUSwan**: The *NUSwan* is a water surface dwelling robot which
    autonomously collects data about the water quality in Singapore’s
    reservoirs with its sensors. This data is relayed to the cloud using
    an LTE connection. However, in large reservoirs, the LTE connection
    may be temporarily unavailable due to lack of coverage.
    `DtnLink` can store pending messages and then send these
    messages when the LTE link is available.

Salient Features
---------

`DtnLink` supports the following features:
- Fragmentation of large messages (payloads)
- Detection of duplicate messages caused due to dropped `DatagramDeliveryNtfs`
- Stop-And-Wait sending to reduce channel congestion
- Short-circuiting to save header space

Usage
---------

After cloning this repo, run any of the simulation scripts using the **UnetStack3** JARs. This project is **not** compatible with older versions of UnetStack.

`DtnLink` can be dropped into any Unet container to enable disruption tolerant communication. On startup `DtnLink` will probe for Link agents supporting the `RELIABILITY` parameter (e.g. `ReliableLink`). All `DatagramReq` sent to `DtnLink` must have a set TTL value or the `DatagramReq` will be refused at the outset.

To use `DtnLink` for multi-hop paths, populate the Routing table of `Router` with `RouteDiscoveryNtf` messages with the link set to `DtnLink` for each hop.

The following parameters of `DtnLink` can be configured by the user at runtime:

- Link Priorities - the order in which `DtnLink` should attempt sending messages
- Order of sending messages (`ARRIVAL`, `EXPIRY`, `RANDOM`)
- Time periods for some functions
- Short-circuiting - send messages to destination without DTN headers

Running Simulations
---------
Using the IntelliJ IDEA IDE is the easiest way to setup your environment for running simulations in the `sim/` directory of this repository. More information about downloading and setting up IDEA with the requisite Unet libs can be found in this [blog post](https://blog.unetstack.net/using-idea-with-unetstack).

Running Tests
---------
`DtnLink` uses a `JUnit` test suite for running regression tests. This uses the methodology of "Black Box" unit testing where we check if `DtnLink` is giving us the expected output for known inputs. To test if `DtnLink` is behaving as expected, run `DtnTest` in the `test/` sub-directory to make sure everything is working correctly.

Useful Links & Documentation
---------
The Request For Comments (RFC) [document](https://www.dropbox.com/s/wudkpl2wkpygkpx/rfc.pdf?dl=0) has more technical details about the project and offers a high-level idea of the design decisions which went into writing `DtnLink`.

My final thesis [presentation](https://www.dropbox.com/s/g0t11y2k8fltjkb/final-presentation.pdf?dl=0), presented to the Acoustic Research Lab on 25 April 2019, has useful diagrams and simulation results which can help in understanding how `DtnLink` works in detail.

License
---------
The source files are distributed under the MIT License (http://opensource.org/licenses/MIT), so feel free to fork and modify `DtnLink` for your projects!
