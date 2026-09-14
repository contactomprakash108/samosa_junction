/**
 * Java adapter that will call the Python AI service over HTTP.
 * The LLM never talks to PostgreSQL. Java remains the transaction boundary.
 */
package com.samosajunction.ai;
