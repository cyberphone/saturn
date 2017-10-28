# Purpose of PSD2
The purpose of PSD2 is enabling third parties creating new *Financial Services* through
open APIs to customers' bank accounts.  Such systems already exist but currently rely of questionable methods
typically requiring consumers giving their login credentials to third parties, which in turn through
"screen-scraping", effectively *impersonate* their clients to their on-line banks.  One of the objectives
with PSD2 is offering dedicated APIs for third party access which do not jeopardize customers' security and privacy.  

Although recently published PSD2 APIs certainly are not identical, they at least share a common "conceptual" model.

This document focuses on PSD2 for *Payments* which in PSD2 are facilitated through PISPs (Payment Initiation Service Providers).
It is in this context worth noting that consumer payment are currently performed through dedicated payment "rails" like EMV, *which do not rely on direct access to accounts by external parties*.

### Certification and Authentication Requirements
Since PSD2 APIs provide access to *sensitive data* in arbitrary banks
without necessarily having a contract, there is a *certification requirement*.
This also implies specific CAs (Certification Authorities) as well as centralized registries
holding data about PSD2 service providers.  A potential problem is that the cost for certification may
be high and may also prove to be somewhat less easy to get acceptance for on a European or global level.

### Quirky User Interfaces
Due to the fact that PSD2 APIs operate at the same trust level as on-line banking,
they (*quite logically*) build on *reusing* the banks' existing (and *arbitrary complex*),
on-line banking authentication solutions and user interfaces.
However, this introduces a *dependency* which card-based payment solutions never had.

If you brought the current PSD2 API concept to the POS (Point of Sale) terminal,
users would most likely have to *manually select* which *bank-specific* "wallet" to use.

### Standardization Hurdles
The lack of a standardized PSD2 API complicates roll-out considerably but may be less of a
problem for financial services than for payments.

### Business Issues
There are already many and *quite successful* "pre-PSD2" mobile payment systems on the market.
Unfortunately, *none of these efforts seem to have enjoyed much success outside of their
original national setting*. This may prove to be difficult to change, because these schemes
(for *scalability* and *interoperability* reasons), usually depend on specific third-party
wallet services, causing "friction" if applied to an international market.
It would effectively be similar to launching a new VISA network.

To accomplish something comparable to VISA et. al., you would most likely have to
create a *PISP aggregation service*.

# Saturn - Optimized for Payments
![Upgraded vision](https://cyberphone.github.io/doc/saturn/supervsdistrib.svg)

To cope with these issues a light-weight tightly-scoped system, coined "[Saturn](https://cyberphone.github.io/doc/saturn/)" was developed which:
- Builds on a slightly enhanced variant of the firmly
established "four corner" model, *obviating the need for PISPs altogether*.
- Maintains a security level surpassing EMV.
- Uses the same API for *all* payment scenarios, be it on-line or at the Gas station.
- Finally unleashes the true power of the *decentralized* banking network!

Saturn, by only dealing with payments, adopts the well-known card paradigm (although the
underlying technology is quite different), making it *technically* feasible having
a single "wallet" for all parties, *exactly like Apple and Google already have*.

In addition to the things stated above, Saturn addresses several other
parameters as well, summarized in the following table:
https://cyberphone.github.io/doc/saturn/saturn--wallet-using-credit-transfer.pdf

For readers with strong interests in technology, a peek in 
https://cyberphone.github.io/doc/saturn/saturn-authorization.pdf
may also be of some use.

### So PISP APIs are Useless for Payments?
Not at all; they are highly useful for payments as a part of "Banking" like:
- Account management (moving money between accounts in different banks).
- Bill payments. 
