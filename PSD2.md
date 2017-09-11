## Saturn features compared to PSD2 APIs

Note: The currently published PSD2 APIs are certainly not identical but seem anyway sharing a common "conceptual" model.

### Purpose of PSD2
The purpose of PSD2 is enabling third parties creating new *financial services* through
open APIs to customers' bank accounts.  This implies essentially the same security and
data integrity levels as the banks themselves need to have.   That such services do not
necessarily have a contract with each bank they interact with, creates a requirement for
*certification*.

### Saturn - "Only" for Payments
Saturn, which has a more limited scope, *exposing no account information to payees*, is therefore
able exploiting a considerably simpler scheme based on the established *four corner* model, obviating
the need for PISPs altogether.

### Trust Establishment
Since PSD2 APIs provide access to potentially sensitive data in arbitrary banks, a PSD2
service provider must authenticate itself as being a certified entity.  This typically
introduces a need for specific CAs (Certification Authorities) as well as public registries
holding data about the services.  A potential problem is that the cost for certification is high
and may also be somewhat less easy to get acceptance for on a European level.

Saturn, on the other hand do not require certification since *there is no third party to certify*.
It does though require that there is a payment-network-wide CA (Certification Authority) providing
participating banks with a "member" certificate.  Equipped with such a certificate,
a bank can enroll any number of payees (Merchants), with minimal efforts, including an efficient
outsourced hosting option.

### User Interface
Since PSD2 APIs operate at the same trust level as banks, they (*quite logically*) build on
*reusing* the banks' existing (and *arbitrary complex*),
on-line banking authentication solutions and user interfaces.
This introduces a *dependency* which EMV-based payment card solutions do not have.
If you brought the PSD2 API concept to POS (Point of Sale) terminal scenarios,
users would most likely have to *manually select* which *bank-specific* "wallet" to use.

Saturn, by only dealing with payments, reuses the well-known card paradigm although the
underlying technology is quite different.  This makes it *technically* possible having
a single "wallet" for all parties, exactly like Apple and Google already have.

### Other Considerations
In addition to the things stated above as well as to the lack of a *Europe-wide* PSD2 API standard,
there is bunch of other differing parameters summarized in the following table:
https://cyberphone.github.io/doc/saturn/saturn--wallet-using-credit-transfer.pdf
