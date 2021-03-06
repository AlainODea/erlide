package org.erlide.wrangler.refactoring.backend.internal;

import java.util.HashMap;
import java.util.Map.Entry;


import org.eclipse.jface.text.IDocument;
import org.erlide.wrangler.refactoring.exception.WranglerException;
import org.erlide.wrangler.refactoring.exception.WranglerRpcParsingException;
import org.erlide.wrangler.refactoring.util.ErlRange;
import org.erlide.wrangler.refactoring.util.IErlRange;
import org.erlide.wrangler.refactoring.util.IRange;
import org.erlide.wrangler.refactoring.util.Range;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class ExpressionPosRpcMessage extends AbstractRpcMessage {

	protected OtpErlangObject syntaxTree;
	protected HashMap<IRange, OtpErlangTuple> positionDefs;

	@Override
	protected void parseRefactoringMessage(OtpErlangTuple resultTuple)
			throws WranglerException {
		try {
			OtpErlangObject wranglerResult = resultTuple.elementAt(1);
			if (resultTuple.elementAt(0).toString().equals("ok")) {
				syntaxTree = ((OtpErlangTuple) wranglerResult).elementAt(0);
				OtpErlangList posDefList = (OtpErlangList) ((OtpErlangTuple) wranglerResult)
						.elementAt(1);

				positionDefs = new HashMap<IRange, OtpErlangTuple>();
				OtpErlangObject[] elements = posDefList.elements();
				for (OtpErlangObject o : elements) {
					OtpErlangTuple value = (OtpErlangTuple) o;
					OtpErlangTuple pos = (OtpErlangTuple) value.elementAt(0);
					try {
						positionDefs.put(new Range(pos), value);
					} catch (OtpErlangRangeException e) {
						e.printStackTrace();
						setUnsuccessful("Failed to parse the result!");
					}
				}
				setSuccessful();

			} else {
				OtpErlangString errorMsg = (OtpErlangString) wranglerResult;
				setUnsuccessful(errorMsg.stringValue());
				return;
			}
		} catch (Exception e) {
			throw new WranglerRpcParsingException(resultTuple.toString());
		}

	}

	public OtpErlangObject getSyntaxTree() {
		return syntaxTree;
	}

	public HashMap<IErlRange, OtpErlangTuple> getPositionDefinitions(
			IDocument doc) {
		HashMap<IErlRange, OtpErlangTuple> ret = new HashMap<IErlRange, OtpErlangTuple>();
		for (Entry<IRange, OtpErlangTuple> r : positionDefs.entrySet()) {
			ret.put(new ErlRange(r.getKey(), doc), r.getValue());
		}

		return ret;
	}

}
