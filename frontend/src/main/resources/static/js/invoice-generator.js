// src/main/resources/static/js/invoice-generator.js

class InvoiceGenerator {
    constructor() {
        // Company details
        this.COMPANY_NAME = "UJJWAL PREM AUTO PARTS AND OIL";
        this.COMPANY_ADDRESS = "123 XYZ Complex, Dondaicha";
        this.COMPANY_CITY = "Dondaicha, Dhule, Maharashtra 400001";
        this.COMPANY_PHONE = "+91-9322505058";
        this.COMPANY_EMAIL = "info@ashirwadcompany.com";
        this.COMPANY_WEBSITE = "www.ujjawalpremautoparts.com";
        this.COMPANY_GST = "27XXXXX1234Z5";

        // Colors
        this.PRIMARY_COLOR = [41, 128, 185];
        this.SECONDARY_COLOR = [52, 73, 94];
        this.LIGHT_GRAY = [245, 245, 245];
        this.BORDER_COLOR = [189, 195, 199];
        this.ORANGE_COLOR = [230, 126, 34];
        this.GREEN_COLOR = [46, 204, 113];
    }

    generateInvoiceNumber(orderId, invoiceType) {
        const now = new Date();
        const prefix = invoiceType === 'SALE' ? 'INV' : 'PINV';
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `${prefix}-${year}${month}${day}-${String(orderId).padStart(4, '0')}`;
    }

    formatDate(dateString) {
        if (!dateString) return '';
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        return `${day}-${month}-${year}`;
    }

    formatCurrency(amount) {
        return `₹${parseFloat(amount).toFixed(2)}`;
    }

    calculateTotalDiscount(orderItems) {
        let totalDiscount = 0;
        orderItems.forEach(item => {
            const product = item.productDto || item.product;
            if (product.discount && product.discount > 0) {
                const itemPrice = product.sellingPrice * item.quantity;
                const discount = itemPrice * (product.discount / 100);
                totalDiscount += discount;
            }
        });
        return totalDiscount;
    }

    convertToWords(amount) {
        const number = Math.floor(amount);
        if (number === 0) return "Zero Rupees Only";
        return `Rupees ${number} Only`;
    }

    async generateInvoice(orders, directPrint = false) {
        console.log("Generating invoice")
        const orderArray = Array.isArray(orders) ? orders : [orders];
        const primaryOrder = orderArray[0];
        const invoiceType = primaryOrder.orderType || 'SALE';

        const { jsPDF } = window.jspdf;
        const doc = new jsPDF({
            orientation: 'portrait',
            unit: 'mm',
            format: 'a4'
        });

        let yPosition = 15;

        yPosition = this.addHeader(doc, invoiceType, yPosition);
        const invoiceNumber = this.generateInvoiceNumber(primaryOrder.orderId, invoiceType);
        yPosition = this.addInvoiceDetails(doc, invoiceNumber, primaryOrder, yPosition);

        if (invoiceType === 'SALE') {
            yPosition = this.addCustomerInfo(doc, primaryOrder, yPosition);
        } else {
            yPosition = this.addSupplierInfo(doc, primaryOrder, yPosition);
        }

        yPosition = this.addItemsTable(doc, orderArray, invoiceType, yPosition);

        let subtotal = 0;
        orderArray.forEach(order => {
            subtotal += order.totalPrice || 0;
        });
        yPosition = this.addSummary(doc, subtotal, primaryOrder, yPosition);
        yPosition = this.addTermsAndConditions(doc, invoiceType, yPosition);
        this.addFooter(doc, invoiceType);

        if (directPrint) {
            this.printDirectly(doc);
        } else {
            doc.save(`${invoiceNumber}.pdf`);
        }

        return doc;
    }

    addHeader(doc, invoiceType, yPosition) {
        // Invoice Title - Centered at top
        const invoiceTitle = invoiceType === 'SALE' ? 'TAX INVOICE' : 'PURCHASE INVOICE';
        doc.setFontSize(24);
        doc.setTextColor(...this.PRIMARY_COLOR);
        doc.setFont(undefined, 'bold');
        doc.text(invoiceTitle, 105, yPosition, { align: 'center' });

        yPosition += 10;

        // Company Name - Centered below title
        doc.setFontSize(16);
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.setFont(undefined, 'bold');
        doc.text(this.COMPANY_NAME, 105, yPosition, { align: 'center' });

        yPosition += 8;

        // Decorative line under company name
        doc.setDrawColor(...this.PRIMARY_COLOR);
        doc.setLineWidth(0.8);
        doc.line(60, yPosition, 150, yPosition);

        yPosition += 8;

        // Company details - Centered
        doc.setFontSize(9);
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.setFont(undefined, 'normal');

        // Address block centered
        doc.text(this.COMPANY_ADDRESS, 105, yPosition, { align: 'center' });
        yPosition += 4;
        doc.text(this.COMPANY_CITY, 105, yPosition, { align: 'center' });
        yPosition += 6;

        // Contact details in two columns
        const leftX = 70;
        const rightX = 140;

        doc.setFontSize(8);
        doc.text(`Phone: ${this.COMPANY_PHONE}`, leftX, yPosition, { align: 'center' });
        doc.text(`GSTIN: ${this.COMPANY_GST}`, rightX, yPosition, { align: 'center' });
        yPosition += 4;

        doc.text(`Email: ${this.COMPANY_EMAIL}`, leftX, yPosition, { align: 'center' });
        doc.text(`Website: ${this.COMPANY_WEBSITE}`, rightX, yPosition, { align: 'center' });
        yPosition += 8;

        // Invoice type badge - Top right corner
        doc.setFontSize(10);
        const typeColor = invoiceType === 'SALE' ? this.GREEN_COLOR : this.ORANGE_COLOR;
        doc.setTextColor(...typeColor);
        doc.setFont(undefined, 'bold');

        // Create a badge background
        const badgeWidth = 25;
        const badgeHeight = 8;
        doc.setFillColor(...typeColor);
        doc.roundedRect(190 - badgeWidth, 10, badgeWidth, badgeHeight, 2, 2, 'F');

        doc.setTextColor(255, 255, 255);
        doc.setFontSize(8);
        doc.text(invoiceType, 190 - badgeWidth/2, 15.5, { align: 'center' });

        // Bottom border line
        doc.setDrawColor(...this.PRIMARY_COLOR);
        doc.setLineWidth(0.5);
        doc.line(20, yPosition, 190, yPosition);

        return yPosition + 8;
    }

    addInvoiceDetails(doc, invoiceNumber, order, yPosition) {
        // Invoice details box
        doc.setFillColor(...this.LIGHT_GRAY);
        doc.setDrawColor(...this.BORDER_COLOR);
        doc.rect(20, yPosition, 170, 12, 'FD');

        doc.setFontSize(9);
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.setFont(undefined, 'bold');

        const invoiceLabel = order.orderType === 'SALE' ? 'Invoice No: ' : 'Purchase Invoice No: ';

        // Left side - Invoice number
        doc.text(invoiceLabel, 25, yPosition + 4);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.PRIMARY_COLOR);
        doc.text(invoiceNumber, 25, yPosition + 8);

        // Right side - Date
        doc.setFont(undefined, 'bold');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.text('Invoice Date:', 140, yPosition + 4);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.PRIMARY_COLOR);
        doc.text(this.formatDate(new Date()), 140, yPosition + 8);

        return yPosition + 18;
    }

    addCustomerInfo(doc, order, yPosition) {
        const customer = order.customer;
        if (!customer) return yPosition;

        // Section headers with improved styling
        doc.setFillColor(...this.PRIMARY_COLOR);
        doc.setTextColor(255, 255, 255);
        doc.setFontSize(9);
        doc.setFont(undefined, 'bold');

        // Header boxes
        doc.roundedRect(20, yPosition, 55, 7, 1, 1, 'F');
        doc.text('BILL TO', 47.5, yPosition + 4.5, { align: 'center' });

        doc.roundedRect(80, yPosition, 55, 7, 1, 1, 'F');
        doc.text('SHIP TO', 107.5, yPosition + 4.5, { align: 'center' });

        doc.roundedRect(140, yPosition, 50, 7, 1, 1, 'F');
        doc.text('ORDER DETAILS', 165, yPosition + 4.5, { align: 'center' });

        yPosition += 7;

        // Content boxes with better spacing
        doc.setDrawColor(...this.BORDER_COLOR);
        doc.setFillColor(255, 255, 255);
        doc.setLineWidth(0.3);

        // Bill To box
        doc.roundedRect(20, yPosition, 55, 32, 1, 1, 'FD');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.setFontSize(9);
        doc.setFont(undefined, 'bold');
        doc.text(customer.name || '', 23, yPosition + 6);
        doc.setFont(undefined, 'normal');
        doc.setFontSize(8);
        if (customer.address) {
            const addressLines = doc.splitTextToSize(customer.address, 50);
            let addressY = yPosition + 10;
            addressLines.forEach(line => {
                doc.text(line, 23, addressY);
                addressY += 4;
            });
        }
        doc.text(`Phone: ${customer.contactNumber || 'N/A'}`, 23, yPosition + 22);
        if (customer.email) doc.text(`Email: ${customer.email}`, 23, yPosition + 26);

        // Ship To box
        doc.roundedRect(80, yPosition, 55, 32, 1, 1, 'FD');
        doc.setFont(undefined, 'bold');
        doc.setFontSize(9);
        doc.text(customer.name || '', 83, yPosition + 6);
        doc.setFont(undefined, 'normal');
        doc.setFontSize(8);
        if (customer.address) {
            const addressLines = doc.splitTextToSize(customer.address, 50);
            let addressY = yPosition + 10;
            addressLines.forEach(line => {
                doc.text(line, 83, addressY);
                addressY += 4;
            });
        }
        doc.text(`Phone: ${customer.contactNumber || 'N/A'}`, 83, yPosition + 22);
        if (customer.email) doc.text(`Email: ${customer.email}`, 83, yPosition + 26);

        // Order Details box
        doc.roundedRect(140, yPosition, 50, 32, 1, 1, 'FD');
        doc.setFontSize(8);
        doc.setFont(undefined, 'bold');
        doc.text('Order ID:', 143, yPosition + 6);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.PRIMARY_COLOR);
        doc.text(`#${order.orderId}`, 143, yPosition + 10);

        doc.setFont(undefined, 'bold');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.text('Order Date:', 143, yPosition + 15);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.PRIMARY_COLOR);
        doc.text(this.formatDate(order.createdAt), 143, yPosition + 19);

        doc.setFont(undefined, 'bold');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.text('Status:', 143, yPosition + 24);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.GREEN_COLOR);
        doc.text(order.orderStatus || '', 143, yPosition + 28);

        return yPosition + 38;
    }

    addSupplierInfo(doc, order, yPosition) {
        const supplier = order.supplier;
        if (!supplier) return yPosition;

        // Section headers with improved styling
        doc.setFillColor(...this.ORANGE_COLOR);
        doc.setTextColor(255, 255, 255);
        doc.setFontSize(9);
        doc.setFont(undefined, 'bold');

        // Header boxes
        doc.roundedRect(20, yPosition, 55, 7, 1, 1, 'F');
        doc.text('SUPPLIER', 47.5, yPosition + 4.5, { align: 'center' });

        doc.setFillColor(...this.PRIMARY_COLOR);
        doc.roundedRect(80, yPosition, 55, 7, 1, 1, 'F');
        doc.text('BILLED TO', 107.5, yPosition + 4.5, { align: 'center' });

        doc.setFillColor(...this.ORANGE_COLOR);
        doc.roundedRect(140, yPosition, 50, 7, 1, 1, 'F');
        doc.text('PURCHASE DETAILS', 165, yPosition + 4.5, { align: 'center' });

        yPosition += 7;

        // Content boxes
        doc.setDrawColor(...this.BORDER_COLOR);
        doc.setFillColor(255, 255, 255);
        doc.setLineWidth(0.3);

        // Supplier box
        doc.roundedRect(20, yPosition, 55, 32, 1, 1, 'FD');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.setFontSize(9);
        doc.setFont(undefined, 'bold');
        doc.text(supplier.name || '', 23, yPosition + 6);
        doc.setFont(undefined, 'normal');
        doc.setFontSize(8);
        if (supplier.address) {
            const addressLines = doc.splitTextToSize(supplier.address, 50);
            let addressY = yPosition + 10;
            addressLines.forEach(line => {
                doc.text(line, 23, addressY);
                addressY += 4;
            });
        }
        doc.text(`Phone: ${supplier.contactNumber || 'N/A'}`, 23, yPosition + 22);
        if (supplier.email) doc.text(`Email: ${supplier.email}`, 23, yPosition + 26);

        // Billed To box (Company details)
        doc.roundedRect(80, yPosition, 55, 32, 1, 1, 'FD');
        doc.setFont(undefined, 'bold');
        doc.setFontSize(9);
        doc.text(this.COMPANY_NAME, 83, yPosition + 6);
        doc.setFont(undefined, 'normal');
        doc.setFontSize(8);
        const companyAddressLines = doc.splitTextToSize(this.COMPANY_ADDRESS, 50);
        let companyAddressY = yPosition + 10;
        companyAddressLines.forEach(line => {
            doc.text(line, 83, companyAddressY);
            companyAddressY += 4;
        });
        doc.text(this.COMPANY_CITY, 83, companyAddressY);
        doc.text(`GSTIN: ${this.COMPANY_GST}`, 83, companyAddressY + 4);

        // Purchase Details box
        doc.roundedRect(140, yPosition, 50, 32, 1, 1, 'FD');
        doc.setFontSize(8);
        doc.setFont(undefined, 'bold');
        doc.text('PO Number:', 143, yPosition + 6);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.ORANGE_COLOR);
        doc.text(`#PO-${order.orderId}`, 143, yPosition + 10);

        doc.setFont(undefined, 'bold');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.text('Purchase Date:', 143, yPosition + 15);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.ORANGE_COLOR);
        doc.text(this.formatDate(order.createdAt), 143, yPosition + 19);

        doc.setFont(undefined, 'bold');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.text('Status:', 143, yPosition + 24);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...this.GREEN_COLOR);
        doc.text(order.orderStatus || '', 143, yPosition + 28);

        return yPosition + 38;
    }

    addItemsTable(doc, orders, invoiceType, yPosition) {
        // Table header with improved styling
        doc.setFillColor(...this.SECONDARY_COLOR);
        doc.setTextColor(255, 255, 255);
        doc.setFontSize(10);
        doc.setFont(undefined, 'bold');

        const headerText = invoiceType === 'SALE' ? 'ITEM DETAILS' : 'PURCHASED ITEMS';
        doc.roundedRect(20, yPosition, 170, 8, 1, 1, 'F');
        doc.text(headerText, 105, yPosition + 5.5, { align: 'center' });

        yPosition += 12;

        const tableHeaders = [
            'S.No.',
            'Item Description',
            'Product Code',
            'Brand',
            'Qty',
            invoiceType === 'SALE' ? 'Rate (₹)' : 'Cost (₹)',
            'Disc%',
            'Amount (₹)'
        ];

        const tableData = [];
        let itemNo = 1;

        orders.forEach(order => {
            order.orderItems.forEach(item => {
                const product = item.productDto || item.product;
                const rate = item.priceAtOrderTime || product.sellingPrice || product.actualPrice;
                const qty = item.quantity;
                const discount = product.discount || 0;
                const amount = rate * qty * (1 - discount / 100);

                tableData.push([
                    itemNo++,
                    `${product.name}${product.attribute && product.attribute !== 'NA' ? '\n' + product.attribute : ''}`,
                    product.productCode || 'N/A',
                    product.brandName || 'Generic',
                    qty.toString(),
                    this.formatCurrency(rate),
                    `${discount}%`,
                    this.formatCurrency(amount)
                ]);
            });
        });

        // Enhanced table styling
        doc.autoTable({
            head: [tableHeaders],
            body: tableData,
            startY: yPosition,
            margin: { left: 20, right: 20 },
            theme: 'striped',
            headStyles: {
                fillColor: this.LIGHT_GRAY,
                textColor: this.SECONDARY_COLOR,
                fontSize: 8,
                fontStyle: 'bold',
                halign: 'center',
                valign: 'middle',
                cellPadding: { top: 3, right: 2, bottom: 3, left: 2 }
            },
            bodyStyles: {
                fontSize: 7,
                textColor: [0, 0, 0],
                cellPadding: { top: 2, right: 2, bottom: 2, left: 2 },
                valign: 'middle'
            },
            alternateRowStyles: {
                fillColor: [250, 250, 250]
            },
            columnStyles: {
                0: { halign: 'center', cellWidth: 12 },    // S.No.
                1: { halign: 'left', cellWidth: 50 },     // Item Description
                2: { halign: 'center', cellWidth: 25 },   // Product Code
                3: { halign: 'center', cellWidth: 20 },   // Brand
                4: { halign: 'center', cellWidth: 12 },   // Qty
                5: { halign: 'right', cellWidth: 20 },    // Rate
                6: { halign: 'center', cellWidth: 12 },   // Disc%
                7: { halign: 'right', cellWidth: 24 }     // Amount
            },
            styles: {
                lineColor: this.BORDER_COLOR,
                lineWidth: 0.2,
                overflow: 'linebreak'
            },
            didParseCell: function(data) {
                // Enhanced cell formatting
                if (data.section === 'head') {
                    data.cell.styles.fillColor = [240, 240, 240];
                    data.cell.styles.textColor = [52, 73, 94];
                }

                // Highlight amount column
                if (data.column.index === 7 && data.section === 'body') {
                    data.cell.styles.fontStyle = 'bold';
                    data.cell.styles.textColor = [41, 128, 185];
                }

                // Format discount column
                if (data.column.index === 6 && data.section === 'body') {
                    if (parseFloat(data.cell.text[0]) > 0) {
                        data.cell.styles.textColor = [230, 126, 34];
                        data.cell.styles.fontStyle = 'bold';
                    }
                }
            }
        });

        return doc.lastAutoTable.finalY + 8;
    }

    addSummary(doc, subtotal, order, yPosition) {
        const totalDiscount = this.calculateTotalDiscount(order.orderItems);
        const grandTotal = subtotal;

        const summaryX = 120;
        const summaryWidth = 70;

        // Summary box background
        doc.setFillColor(248, 249, 250);
        doc.setDrawColor(...this.BORDER_COLOR);
        doc.roundedRect(summaryX - 5, yPosition - 5, summaryWidth + 10, 35, 2, 2, 'FD');

        doc.setFontSize(8);
        doc.setTextColor(...this.SECONDARY_COLOR);

        if (totalDiscount > 0) {
            doc.text('Total Discount:', summaryX, yPosition);
            doc.setTextColor(...this.ORANGE_COLOR);
            doc.text(`-${this.formatCurrency(totalDiscount)}`, summaryX + summaryWidth, yPosition, { align: 'right' });
            yPosition += 6;
        }

        // Grand total with emphasis
        doc.setFillColor(...this.PRIMARY_COLOR);
        doc.setDrawColor(...this.PRIMARY_COLOR);
        doc.setLineWidth(0.5);
        doc.roundedRect(summaryX, yPosition, summaryWidth, 10, 2, 2, 'FD');

        doc.setFontSize(10);
        doc.setFont(undefined, 'bold');
        doc.setTextColor(255, 255, 255);
        doc.text('GRAND TOTAL:', summaryX + 3, yPosition + 6.5);
        doc.text(this.formatCurrency(grandTotal), summaryX + summaryWidth - 3, yPosition + 6.5, { align: 'right' });

        yPosition += 15;

        // Amount in words
        doc.setFillColor(...this.LIGHT_GRAY);
        doc.roundedRect(20, yPosition, 170, 8, 1, 1, 'F');
        doc.setFontSize(8);
        doc.setFont(undefined, 'italic');
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.text(`Amount in words: ${this.convertToWords(grandTotal)}`, 25, yPosition + 5.5);

        return yPosition + 15;
    }

    addTermsAndConditions(doc, invoiceType, yPosition) {
        if (yPosition > 240) {
            doc.addPage();
            yPosition = 20;
        }

        // Terms header
        doc.setFillColor(...this.SECONDARY_COLOR);
        doc.setTextColor(255, 255, 255);
        doc.setFontSize(9);
        doc.setFont(undefined, 'bold');
        doc.roundedRect(20, yPosition, 170, 7, 1, 1, 'F');
        doc.text('TERMS & CONDITIONS', 25, yPosition + 5);

        yPosition += 7;

        // Terms content
        doc.setFillColor(252, 252, 252);
        doc.setDrawColor(...this.BORDER_COLOR);
        doc.roundedRect(20, yPosition, 170, 25, 1, 1, 'FD');

        doc.setFont(undefined, 'normal');
        doc.setFontSize(7);
        doc.setTextColor(...this.SECONDARY_COLOR);

        if (invoiceType === 'SALE') {
            doc.text('• Goods once sold will not be taken back or exchanged', 25, yPosition + 5);
            doc.text('• Payment terms: As per agreed terms', 25, yPosition + 9);
            doc.text('• Any disputes subject to local jurisdiction only', 25, yPosition + 13);
            doc.text('• E. & O.E. (Errors and Omissions Excepted)', 25, yPosition + 17);
        } else {
            doc.text('• All goods received in good condition and as per specifications', 25, yPosition + 5);
            doc.text('• Payment terms as per purchase agreement', 25, yPosition + 9);
            doc.text('• Quality and warranty as per supplier terms', 25, yPosition + 13);
            doc.text('• Subject to supplier\'s terms and conditions', 25, yPosition + 17);
            doc.text('• E. & O.E. (Errors and Omissions Excepted)', 25, yPosition + 21);
        }

        return yPosition + 32;
    }

    addFooter(doc, invoiceType) {
        const pageHeight = doc.internal.pageSize.height;
        let yPosition = pageHeight - 35;

        // Signature section
        const leftSigX = 60;
        const rightSigX = 150;

        // Signature lines
        doc.setDrawColor(...this.BORDER_COLOR);
        doc.setLineWidth(0.3);
        doc.line(30, yPosition, 90, yPosition);
        doc.line(120, yPosition, 180, yPosition);

        doc.setFontSize(8);
        doc.setTextColor(...this.SECONDARY_COLOR);
        doc.setFont(undefined, 'normal');

        const leftLabel = invoiceType === 'SALE' ? "Customer Signature" : "Received By";
        const rightLabel = invoiceType === 'SALE' ? "Authorized Signatory" : "Verified By";

        doc.text(leftLabel, leftSigX, yPosition + 5, { align: 'center' });
        doc.text(rightLabel, rightSigX, yPosition + 5, { align: 'center' });

        yPosition += 12;

        // Footer text
        doc.setFontSize(6);
        doc.setTextColor(128, 128, 128);
        doc.text('This is a computer generated invoice and does not require physical signature', 105, yPosition, { align: 'center' });

        yPosition += 5;

        // Thank you message
        doc.setFontSize(8);
        doc.setFont(undefined, 'bold');
        doc.setTextColor(...this.PRIMARY_COLOR);
        const thankYouMessage = invoiceType === 'SALE' ? 'Thank you for your business!' : 'Thank you for your supply!';
        doc.text(thankYouMessage, 105, yPosition, { align: 'center' });
    }

    downloadAndPrint(doc, invoiceNumber) {
        // For browsers that need download method
        doc.save(`${invoiceNumber}.pdf`);

        // Optional: Show instructions for manual printing
        setTimeout(() => {
            alert('PDF downloaded. Please open the file and use Ctrl+P to print.');
        }, 500);
    }

    printDirectly(doc) {
        const pdfBlob = doc.output('blob');
        const pdfUrl = URL.createObjectURL(pdfBlob);

        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.src = pdfUrl;

        document.body.appendChild(iframe);

        iframe.onload = function() {
            setTimeout(() => {
                iframe.contentWindow.print();

                setTimeout(() => {
                    document.body.removeChild(iframe);
                    URL.revokeObjectURL(pdfUrl);
                }, 60000);
            }, 100);
        };
    }
}

// Global function to print invoice from anywhere in the application
window.printInvoice = async function(orderId, options = {}) {
    try {
        // Detect browser
        const isEdge = navigator.userAgent.indexOf('Edge') > -1 || navigator.userAgent.indexOf('Edg/') > -1;
        const isChrome = navigator.userAgent.indexOf('Chrome') > -1 && !isEdge;
        const isSafari = navigator.userAgent.indexOf('Safari') > -1 && !isChrome && !isEdge;

        // Show loading state
        const button = event ? event.target.closest('button') : null;
        let originalContent = '';

        if (button) {
            originalContent = button.innerHTML;
            button.innerHTML = '<i class="bi bi-hourglass-split animate-spin mr-2"></i>Generating...';
            button.disabled = true;
        }

        // Fetch order data
        const response = await fetch(`/api/orders/${orderId}`);
        if (!response.ok) {
            throw new Error('Failed to fetch order data');
        }
        const orderData = await response.json();

        // Generate invoice
        const generator = new InvoiceGenerator();

        // For Edge or if specified, use download method
        if (isEdge || options.forceDownload) {
            const invoiceNumber = generator.generateInvoiceNumber(orderData.orderId, orderData.orderType);
            const doc = await generator.generateInvoice(orderData, false);
            generator.downloadAndPrint(doc, invoiceNumber);
        } else {
            // For Chrome and other browsers, use direct print
            await generator.generateInvoice(orderData, true);
        }

        // Restore button state
        if (button) {
            button.innerHTML = originalContent;
            button.disabled = false;
        }
    } catch (error) {
        console.error('Error printing invoice:', error);
        alert('Error generating invoice. Please try again.');

        // Restore button state
        if (event) {
            const button = event.target.closest('button');
            if (button) {
                button.disabled = false;
            }
        }
    }
};

// Alternative function that accepts order data directly
window.printInvoiceWithData = function(orderData, options = {}) {
    try {
        const generator = new InvoiceGenerator();

        // Detect Edge browser
        const isEdge = navigator.userAgent.indexOf('Edge') > -1 || navigator.userAgent.indexOf('Edg/') > -1;

        if (isEdge || options.forceDownload) {
            const invoiceNumber = generator.generateInvoiceNumber(orderData.orderId, orderData.orderType);
            const doc = generator.generateInvoice(orderData, false);
            generator.downloadAndPrint(doc, invoiceNumber);
        } else {
            generator.generateInvoice(orderData, true);
        }
    } catch (error) {
        console.error('Error printing invoice:', error);
        alert('Error generating invoice. Please try again.');
    }
};

// Helper function to check browser compatibility
window.checkPrintCompatibility = function() {
    const isEdge = navigator.userAgent.indexOf('Edge') > -1 || navigator.userAgent.indexOf('Edg/') > -1;
    const isIE = navigator.userAgent.indexOf('MSIE') > -1 || navigator.userAgent.indexOf('Trident/') > -1;

    if (isIE) {
        alert('Internet Explorer is not supported. Please use Edge, Chrome, or Firefox.');
        return false;
    }

    if (isEdge) {
        console.log('Edge detected - using download method for printing');
    }

    return true;
};