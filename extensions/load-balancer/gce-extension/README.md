# Apache Stratos GCE Extension

Apache Stratos GCE extension is a load balancer extension for Google Compute Engine Load balancer.
It is an executable program which can manage Google Compute Engine Load balancing according to the topology,
composite application model and tenant application signups information received from Stratos via
the message broker.

## How it works
1. Wait for the complete topology event message to initialize the topology.
2. Configure and create relevant forwarding rules, target pools and health checks in GCE.
3. Listen to topology, application, application signup events.
4. Reconfigure the load balancer with the new topology configuration.

## Installation
Please refer INSTALL.md for information on a quick installation process.

Please refer bellow document for information on a detailed installation process.

https://docs.google.com/document/d/1a2ZptPScpjuavfpxVu1R1GC7R95jjzHo3L372zL2bRY/edit