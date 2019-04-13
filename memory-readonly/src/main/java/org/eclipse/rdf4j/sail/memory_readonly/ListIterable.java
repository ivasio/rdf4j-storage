/* @formatter:off */
/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory_readonly;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.util.iterators.EmptyIterator;

import java.util.Iterator;
import java.util.List;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class ListIterable implements Iterable<Statement> {


	private final boolean needsFurtherFiltering;
	private final List<Statement> statements;

	ListIterable(List<Statement> statements, boolean needsFurtherFiltering) {
		this.statements = statements;
		this.needsFurtherFiltering = needsFurtherFiltering;
	}

	public boolean isNeedsFurtherFiltering() {
		return needsFurtherFiltering;
	}

	@Override
	public Iterator<Statement> iterator() {
		return new Iterator<Statement>() {


			Iterator<Statement> iterator = statements.iterator();

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Statement next() {
				return iterator.next();
			}
		};
	}

	static class EmptyListIterable extends ListIterable {

		EmptyListIterable(List<Statement> statements, boolean needsFurtherFiltering){
			super(statements, needsFurtherFiltering);

		}

		public EmptyListIterable() {
			super(null, false);
		}

		@Override
		public Iterator<Statement> iterator() {
			return new EmptyIterator<>();
		}
	}

}


