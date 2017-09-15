# Purpose of PSD2
The purpose of PSD2 is enabling third parties creating new *financial services* through
open APIs to customers' bank accounts.

Note: Although the currently published PSD2 APIs certainly are not identical, they seem anyway sharing a common "conceptual" model.

This document focuses on PSD2 for *payments* which in PSD2 is accomplished through PISPs (Payment Initiation Service Providers).

### Certification and Authentication Requirements
Since PSD2 APIs provide access to *sensitive data* in arbitrary banks
without necessarily having a contract, creates a requirement for *certification*.
This also implies specific CAs (Certification Authorities) as well as centralized registries
holding data about PSD2 service providers.  A potential problem is that the cost for certification may
be high and may also prove to be somewhat less easy to get acceptance for on a European or global level.

### User Interface Issues
Due to the fact that PSD2 APIs operate at the same trust level as banks,
they (*quite logically*) build on *reusing* the banks' existing (and *arbitrary complex*),
on-line banking authentication solutions and user interfaces.
However, this introduces a *dependency* which card-based payment solutions never had.
If you brought the current PSD2 API concept to the POS (Point of Sale) terminal,
users would most likely have to *manually select* which *bank-specific* "wallet" to use.

### Not yet Standardized
The lack of a standardized PSD2 API complicates roll-out considerably but may be less of a
problem for financial services than for payments.

## Saturn - Optimized for Payments
To cope with these issues a light-weight tightly-scoped system, coined "Saturn" was developed which
- does not depend on third party certification since *there is no third party to certify*
- does not expose *any* private information to external parties
- builds on a slightly enhanced variant of the firmly
established *four corner* model, obviating the need for PISPs altogether

Saturn, by only dealing with payments, adopts the well-known card paradigm (although the
underlying technology is quite different), making it *technically* feasible having
a single "wallet" for all parties, *exactly like Apple and Google already have*.

In addition to the things stated above, there is bunch of other
parameters summarized in the following table:
https://cyberphone.github.io/doc/saturn/saturn--wallet-using-credit-transfer.pdf

For readers with strong interests in technology, a peek in 
https://cyberphone.github.io/doc/saturn/saturn-authorization.pdf
may also be of some use.
