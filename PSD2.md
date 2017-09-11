## Saturn versus PSD2 APIs

Note: Although the currently published PSD2 APIs certainly are not identical, they seem anyway sharing a common "conceptual" model.

### Purpose of PSD2
The purpose of PSD2 is enabling third parties creating new *financial services* through
open APIs to customers' bank accounts.  This implies essentially the same security and
data integrity levels as the banks themselves need to have.   That such services do not
necessarily have a contract with each bank they interact with, creates a requirement for
*certification*.

### Saturn - Payments Only
Saturn, which has a more limited scope, *exposing no account information to payees*, is therefore
able exploiting a considerably simpler scheme based on a slightly enhanced variant of the firmly
established *four corner* model, obviating the need for PISPs altogether.

### Trust Establishment
Since PSD2 APIs provide access to potentially sensitive data in arbitrary banks, a PSD2
service provider must authenticate itself to banks in a trustworthy manner.  This typically
demands specific CAs (Certification Authorities) as well as centralized registries
holding data about PSD2 service providers.  A potential problem is that the cost for certification may
be high and may also prove to be somewhat less easy to get acceptance for on a European or global level.

Saturn, on the other hand, does not depend on third party certification since *there is no third party to certify*.
It does though *mandate* a payment-network-wide CA providing
participating banks with "member" certificates.  Equipped with such a certificate,
a bank can with limited efforts enroll any number of payees (Merchants), alternatively
rely on a *built-in*, highly scalable outsourced hosting option.

### User Interfaces
Due to the fact that PSD2 APIs operate at the same trust level as banks, they (*quite logically*) build on
*reusing* the banks' existing (and *arbitrary complex*),
on-line banking authentication solutions and user interfaces.
However, this introduces a *dependency* which card-based payment solutions never had.
If you brought the current PSD2 API concept to the POS (Point of Sale) terminal,
users would most likely have to *manually select* which *bank-specific* "wallet" to use.

Saturn, by only dealing with payments, adopted the well-known card paradigm (although the
underlying technology is quite different), making it *technically* feasible having
a single "wallet" for all parties, *exactly like Apple and Google already have*.

### Other Considerations
In addition to the things stated above as well as to the lack of a *Europe-wide* PSD2 API standard,
there is bunch of other differing parameters summarized in the following table:
https://cyberphone.github.io/doc/saturn/saturn--wallet-using-credit-transfer.pdf

For readers with strong interests in technology, a peek in 
https://cyberphone.github.io/doc/saturn/saturn-authorization.pdf
may also be of some use.
